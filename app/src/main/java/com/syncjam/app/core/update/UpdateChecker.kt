package com.syncjam.app.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.net.URL

private const val GITHUB_REPO = "Pcf1337-hash/SyncJam"
const val APP_VERSION = "1.7.0"

@Serializable
data class AppRelease(
    val version: String,
    val tagName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val apkUrl: String,
    val apkSizeBytes: Long
)

private fun compareVersions(a: String, b: String): Int {
    val partsA = a.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
    val partsB = b.trimStart('v').split(".").map { it.toIntOrNull() ?: 0 }
    val maxLen = maxOf(partsA.size, partsB.size)
    for (i in 0 until maxLen) {
        val diff = (partsA.getOrElse(i) { 0 }) - (partsB.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return 0
}

suspend fun checkForUpdate(httpClient: HttpClient): AppRelease? = withContext(Dispatchers.IO) {
    try {
        val response = httpClient.get("https://api.github.com/repos/$GITHUB_REPO/releases/latest") {
            header("Accept", "application/vnd.github.v3+json")
        }
        val json = Json { ignoreUnknownKeys = true }
        val obj = response.body<JsonObject>()

        val tagName = obj["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
        if (compareVersions(tagName, APP_VERSION) <= 0) return@withContext null

        val assets = obj["assets"]?.jsonArray ?: return@withContext null
        val apkAsset = assets.firstOrNull { asset ->
            asset.jsonObject["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true
        }?.jsonObject

        AppRelease(
            version = tagName,
            tagName = tagName,
            releaseNotes = obj["body"]?.jsonPrimitive?.content?.trim() ?: "",
            publishedAt = obj["published_at"]?.jsonPrimitive?.content ?: "",
            apkUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.content ?: "",
            apkSizeBytes = apkAsset?.get("size")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        )
    } catch (e: Exception) {
        null
    }
}

suspend fun downloadApk(
    apkUrl: String,
    destFile: File,
    onProgress: (Float) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = URL(apkUrl)
        val connection = url.openConnection()
        connection.connect()
        val totalBytes = connection.contentLengthLong

        destFile.parentFile?.mkdirs()
        var downloaded = 0L
        connection.getInputStream().use { input: InputStream ->
            destFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0) onProgress(downloaded.toFloat() / totalBytes)
                }
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}

fun installApk(context: Context, apkFile: File) {
    // Android 8+: check install-from-unknown-sources permission first
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(settingsIntent)
        return
    }
    val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
    } else {
        Uri.fromFile(apkFile)
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
