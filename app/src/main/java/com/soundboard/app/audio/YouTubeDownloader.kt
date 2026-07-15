package com.soundboard.app.audio

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class YouTubeDownloader(private val context: Context) {

    private val soundsDir = File(context.filesDir, "sounds").also { it.mkdirs() }

    suspend fun downloadAudio(url: String, onProgress: (Float) -> Unit): Result<DownloadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val info = YoutubeDL.getInfo(url)
                val title = info.title ?: info.fulltitle ?: "unknown"

                val request = YoutubeDLRequest(url)
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("-o", "${soundsDir.absolutePath}/%(title)s.%(ext)s")

                YoutubeDL.execute(request, null) { progress, _, _ ->
                    onProgress(progress)
                }

                val outputFile = File(soundsDir, "$title.mp3")
                if (!outputFile.exists()) {
                    val existing = soundsDir.listFiles()?.maxByOrNull { it.lastModified() }
                        ?: return@withContext Result.failure(Exception("Download failed: output file not found"))
                    Result.success(DownloadResult(title, existing))
                } else {
                    Result.success(DownloadResult(title, outputFile))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    data class DownloadResult(val title: String, val file: File)
}
