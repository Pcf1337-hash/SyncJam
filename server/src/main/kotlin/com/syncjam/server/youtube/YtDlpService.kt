package com.syncjam.server.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
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

class YtDlpService(
    private val downloadDir: String = System.getenv("YTDLP_DOWNLOAD_DIR") ?: "/app/downloads"
) {
    private val logger = LoggerFactory.getLogger(YtDlpService::class.java)

    init {
        File(downloadDir).mkdirs()
    }

    suspend fun getInfo(youtubeUrl: String): YtDlpInfo? = withContext(Dispatchers.IO) {
        try {
            extractYouTubeId(youtubeUrl) ?: return@withContext null
            val process = ProcessBuilder(
                "yt-dlp",
                "--no-playlist",
                "--print", "%(id)s|%(title)s|%(uploader)s|%(duration)s|%(thumbnail)s",
                youtubeUrl
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
            ).apply {
                redirectErrorStream(false)
                directory(File(downloadDir))
            }.start()

            // Drain stdout (print-json output) to prevent blocking
            process.inputStream.bufferedReader().readText()
            val exited = process.waitFor(300, TimeUnit.SECONDS) // 5 min timeout
            if (!exited) {
                process.destroyForcibly()
                logger.error("yt-dlp timeout for $youtubeUrl")
                return@withContext null
            }
            if (process.exitValue() != 0) {
                val err = process.errorStream.bufferedReader().readText()
                logger.error("yt-dlp failed (${process.exitValue()}): $err")
                return@withContext null
            }

            val downloadedFile = findExistingFile(ytId) ?: return@withContext null
            val info = getInfo(youtubeUrl)

            YtDlpResult(
                id = ytId,
                title = info?.title ?: ytId,
                artist = info?.uploader ?: "YouTube",
                durationMs = (info?.duration ?: 0L) * 1000L,
                filePath = downloadedFile.absolutePath,
                thumbnailUrl = info?.thumbnail,
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
