package com.soundboard.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.services.ServiceList
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class YouTubeDownloader(private val context: Context) {

    private val soundsDir = File(context.filesDir, "sounds").also { it.mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadAudio(url: String, onProgress: (Float) -> Unit): Result<DownloadResult> {
        return withContext(Dispatchers.IO) {
            try {
                ensureInitialized()

                val service = ServiceList.YouTube
                val linkHandler = service.streamLHFactory.fromUrl(url)
                val extractor = service.getStreamExtractor(linkHandler)
                extractor.fetchPage()

                val audioStreams = extractor.audioStreams ?: emptyList()
                if (audioStreams.isEmpty()) {
                    return@withContext Result.failure(Exception("No audio streams found"))
                }

                val bestAudio = audioStreams.maxByOrNull { it.averageBitrate }
                    ?: return@withContext Result.failure(Exception("No suitable audio stream"))

                val audioUrl = bestAudio.url ?: return@withContext Result.failure(Exception("Audio URL is empty"))
                val title = sanitizeFileName(extractor.name ?: "audio")
                val extension = getExtension(bestAudio.format?.mimeType ?: "audio/webm")

                val file = File(soundsDir, "$title.$extension")
                downloadFile(audioUrl, file, onProgress)

                if (!file.exists() || file.length() == 0L) {
                    return@withContext Result.failure(Exception("Downloaded file is empty"))
                }

                Result.success(DownloadResult(title, file))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun downloadFile(audioUrl: String, file: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder()
            .url(audioUrl)
            .header("User-Agent", USER_AGENT)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body ?: throw IOException("Empty response body")

        val totalBytes = body.contentLength()
        val inputStream = body.byteStream()
        val outputStream = file.outputStream()

        val buffer = ByteArray(8192)
        var bytesRead: Long = 0
        var bytes = inputStream.read(buffer)
        while (bytes >= 0) {
            outputStream.write(buffer, 0, bytes)
            bytesRead += bytes
            if (totalBytes > 0) {
                onProgress(bytesRead.toFloat() / totalBytes)
            }
            bytes = inputStream.read(buffer)
        }

        outputStream.close()
        inputStream.close()
    }

    private fun ensureInitialized() {
        if (!isInitialized) {
            NewPipe.init(OkHttpDownloader())
            isInitialized = true
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[" + Regex.escape("""\/:*?"<>|""") + "]"), "_")
            .take(100)
    }

    private fun getExtension(mimeType: String): String {
        return when {
            mimeType.contains("webm") || mimeType.contains("opus") -> "webm"
            mimeType.contains("mp4") || mimeType.contains("m4a") || mimeType.contains("aac") -> "m4a"
            mimeType.contains("mp3") -> "mp3"
            mimeType.contains("ogg") -> "ogg"
            mimeType.contains("flac") -> "flac"
            mimeType.contains("wav") -> "wav"
            else -> "webm"
        }
    }

    data class DownloadResult(val title: String, val file: File)

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        private var isInitialized = false
    }
}
