package com.syncjam.server.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable
data class YtDlpResult(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val filePath: String,
    val thumbnailUrl: String?,
    val youtubeId: String,
    val fileSize: Long
)

@Serializable
data class YtDlpInfo(
    val id: String,
    val title: String,
    val uploader: String,
    val duration: Long, // seconds
    val thumbnail: String?
)

@Serializable
private data class YtDlpJsonOutput(
    val id: String = "",
    val title: String = "",
    val uploader: String? = null,
    val channel: String? = null,
    val duration: Double = 0.0,
    val thumbnail: String? = null
)

private val ytDlpJson = Json { ignoreUnknownKeys = true }

class YtDlpService(
    private val downloadDir: String = System.getenv("YTDLP_DOWNLOAD_DIR") ?: "/app/downloads",
    private val cookiesPath: String? = System.getenv("YTDLP_COOKIES_PATH")
) {
    private val logger = LoggerFactory.getLogger(YtDlpService::class.java)

    init {
        File(downloadDir).mkdirs()
    }

    /** Appends --cookies flag if a cookie file is configured and exists. */
    private fun List<String>.withCookies(): List<String> {
        val path = cookiesPath ?: return this
        if (!File(path).exists()) return this
        return this + listOf("--cookies", path)
    }

    suspend fun getInfo(youtubeUrl: String): YtDlpInfo? = withContext(Dispatchers.IO) {
        try {
            extractYouTubeId(youtubeUrl) ?: return@withContext null
            val process = ProcessBuilder(
                listOf(
                    "yt-dlp",
                    "--no-playlist",
                    "--print", "%(id)s|%(title)s|%(uploader)s|%(duration)s|%(thumbnail)s",
                    youtubeUrl
                ).withCookies()
            ).apply {
                redirectErrorStream(true)
            }.start()

            val output = process.inputStream.bufferedReader().readLine() ?: return@withContext null
            val exited = process.waitFor(30, TimeUnit.SECONDS)
            if (!exited || process.exitValue() != 0) return@withContext null

            val parts = output.split("|")
            if (parts.size < 4) return@withContext null

            YtDlpInfo(
                id = parts[0].trim(),
                title = parts[1].trim(),
                uploader = parts[2].trim(),
                duration = parts[3].trim().toLongOrNull() ?: 0L,
                thumbnail = parts.getOrNull(4)?.trim()?.takeIf { it != "NA" }
            )
        } catch (e: Exception) {
            logger.error("yt-dlp getInfo failed for $youtubeUrl: ${e.message}")
            null
        }
    }

    suspend fun download(youtubeUrl: String): YtDlpResult? = withContext(Dispatchers.IO) {
        try {
            val ytId = extractYouTubeId(youtubeUrl) ?: return@withContext null
            val outputTemplate = "$downloadDir/$ytId.%(ext)s"

            // Check if already downloaded — skip re-download
            val existingFile = findExistingFile(ytId)
            if (existingFile != null) {
                logger.info("Already downloaded: $ytId at ${existingFile.absolutePath}")
                val info = getInfo(youtubeUrl)
                return@withContext YtDlpResult(
                    id = ytId,
                    title = info?.title ?: ytId,
                    artist = info?.uploader ?: "YouTube",
                    durationMs = (info?.duration ?: 0L) * 1000L,
                    filePath = existingFile.absolutePath,
                    thumbnailUrl = info?.thumbnail,
                    youtubeId = ytId,
                    fileSize = existingFile.length()
                )
            }

            logger.info("Downloading: $youtubeUrl")
            val process = ProcessBuilder(
                listOf(
                    "yt-dlp",
                    "--no-playlist",
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "192K",
                    "--embed-thumbnail",
                    "--add-metadata",
                    "--output", outputTemplate,
                    "--print-json",
                    youtubeUrl
                ).withCookies()
            ).apply {
                redirectErrorStream(false)
                directory(File(downloadDir))
            }.start()

            // Drain stderr in background thread to prevent pipe buffer deadlock
            var stderrContent = ""
            val stderrThread = Thread { stderrContent = process.errorStream.bufferedReader().readText() }
                .also { it.isDaemon = true; it.start() }

            // Capture --print-json stdout output for metadata extraction
            val jsonOutput = process.inputStream.bufferedReader().readText().trim()
            val exited = process.waitFor(300, TimeUnit.SECONDS) // 5 min timeout
            stderrThread.join(5000)

            if (!exited) {
                process.destroyForcibly()
                logger.error("yt-dlp timeout for $youtubeUrl")
                return@withContext null
            }
            if (process.exitValue() != 0) {
                logger.error("yt-dlp failed (${process.exitValue()}): $stderrContent")
                return@withContext null
            }

            val downloadedFile = findExistingFile(ytId) ?: return@withContext null

            // Parse --print-json metadata directly (avoid a second yt-dlp call)
            val parsedInfo: YtDlpJsonOutput? = runCatching {
                jsonOutput.lines()
                    .map { it.trim() }
                    .filter { it.startsWith("{") }
                    .firstNotNullOfOrNull { line ->
                        runCatching { ytDlpJson.decodeFromString<YtDlpJsonOutput>(line) }.getOrNull()
                    }
            }.getOrNull()

            val title = parsedInfo?.title?.takeIf { it.isNotBlank() } ?: ytId
            val artist = parsedInfo?.let { it.uploader ?: it.channel }?.takeIf { it.isNotBlank() } ?: "YouTube"
            val durationMs = parsedInfo?.duration?.let { (it * 1000.0).toLong() } ?: 0L
            val thumbnailUrl = parsedInfo?.thumbnail?.takeIf { it.isNotBlank() && it != "none" }

            logger.info("Parsed metadata for $ytId: title='$title', artist='$artist', duration=${durationMs}ms")

            YtDlpResult(
                id = ytId,
                title = title,
                artist = artist,
                durationMs = durationMs,
                filePath = downloadedFile.absolutePath,
                thumbnailUrl = thumbnailUrl,
                youtubeId = ytId,
                fileSize = downloadedFile.length()
            )
        } catch (e: Exception) {
            logger.error("yt-dlp download failed for $youtubeUrl: ${e.message}")
            null
        }
    }

    fun getFilePath(youtubeId: String): File? = findExistingFile(youtubeId)

    private fun findExistingFile(ytId: String): File? {
        val dir = File(downloadDir)
        return dir.listFiles()?.firstOrNull {
            it.nameWithoutExtension == ytId && it.extension in listOf("mp3", "m4a", "opus", "webm", "ogg")
        }
    }

    fun isYouTubeUrl(url: String): Boolean =
        url.contains("youtube.com") || url.contains("youtu.be")

    fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("youtube\\.com/shorts/([a-zA-Z0-9_-]{11})")
        )
        return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }
    }
}
