package com.soundboard.app.audio

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class YouTubeDownloader(private val context: Context) {

    private val soundsDir = File(context.filesDir, "sounds").also { it.mkdirs() }

    suspend fun downloadAudio(url: String, onProgress: (Float) -> Unit): Result<DownloadResult> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val request = YoutubeDLRequest(url)
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("-o", "${soundsDir.absolutePath}/%(title)s.%(ext)s")

                YoutubeDL.getInstance().execute(request, object : YoutubeDL.Callback {
                    override fun onProgress(progress: Float, etaInSeconds: Long) {
                        onProgress(progress)
                    }

                    override fun onSuccess(response: YoutubeDLResponse) {
                        val file = File(response.outFilePath)
                        val title = file.nameWithoutExtension
                        continuation.resume(Result.success(DownloadResult(title, file)))
                    }

                    override fun onError(error: Exception) {
                        continuation.resume(Result.failure(error))
                    }
                })
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    data class DownloadResult(val title: String, val file: File)
}
