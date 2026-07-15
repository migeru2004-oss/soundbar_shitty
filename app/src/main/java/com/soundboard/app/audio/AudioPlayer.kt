package com.soundboard.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.soundboard.app.model.SoundItem

class AudioPlayer(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .build()

    private val loadedIds = mutableMapOf<String, Int>()

    fun play(sound: SoundItem) {
        val soundId = loadedIds.getOrPut(sound.id) {
            soundPool.load(sound.filePath, 1)
        }
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
        loadedIds.clear()
    }
}
