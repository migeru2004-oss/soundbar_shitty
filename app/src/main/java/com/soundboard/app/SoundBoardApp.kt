package com.soundboard.app

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundBoardApp : Application() {

    companion object {
        private const val TAG = "SoundBoardApp"
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@SoundBoardApp)
                Log.d(TAG, "YouTubeDL initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init YouTubeDL", e)
            }
            try {
                FFmpeg.getInstance().init(this@SoundBoardApp)
                Log.d(TAG, "FFmpeg initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init FFmpeg", e)
            }
        }
    }
}
