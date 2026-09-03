package com.example.blueprintai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val isDownloading: Boolean = false,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val error: String? = null,
    val isCompleted: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes.toFloat() else 0f
}

@Singleton
class ModelDownloader @Inject constructor() {

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    suspend fun downloadModel(
        urlStr: String = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        targetPath: String = "/storage/emulated/0/Download/AI_Models/gemma-4-E2B-it.litertlm"
    ) = withContext(Dispatchers.IO) {
        if (_downloadProgress.value.isDownloading) return@withContext

        _downloadProgress.value = DownloadProgress(isDownloading = true)

        try {
            val targetFile = File(targetPath)
            targetFile.parentFile?.mkdirs()
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
            if (tempFile.exists()) tempFile.delete()

            var currentUrl = urlStr
            var connection: HttpURLConnection? = null
            var redirects = 0

            while (redirects < 5) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) BlueprintAI/1.0")
                    instanceFollowRedirects = true
                }

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    currentUrl = connection.getHeaderField("Location") ?: break
                    connection.disconnect()
                    redirects++
                } else {
                    break
                }
            }

            val conn = connection ?: throw Exception("Failed to connect")
            if (conn.responseCode !in 200..299) {
                throw Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}")
            }

            val totalBytes = conn.contentLengthLong
            val input: InputStream = conn.inputStream
            val output = FileOutputStream(tempFile)

            val buffer = ByteArray(256 * 1024)
            var bytesDownloaded = 0L
            var lastUpdate = 0L

            try {
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    bytesDownloaded += read

                    val now = System.currentTimeMillis()
                    if (now - lastUpdate >= 250) {
                        lastUpdate = now
                        _downloadProgress.value = DownloadProgress(
                            isDownloading = true,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes
                        )
                    }
                }
                output.flush()
            } finally {
                output.close()
                input.close()
                conn.disconnect()
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                throw Exception("Failed to move downloaded file to destination: $targetPath")
            }

            _downloadProgress.value = DownloadProgress(
                isDownloading = false,
                bytesDownloaded = bytesDownloaded,
                totalBytes = bytesDownloaded,
                isCompleted = true
            )
        } catch (e: Exception) {
            _downloadProgress.value = DownloadProgress(
                isDownloading = false,
                error = e.localizedMessage ?: "Download failed"
            )
        }
    }

    fun resetProgress() {
        _downloadProgress.value = DownloadProgress()
    }
}
