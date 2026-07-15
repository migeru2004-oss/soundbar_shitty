package com.soundboard.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soundboard.app.model.SoundItem
import com.soundboard.app.model.SourceType
import java.io.File
import java.util.UUID

class SoundRepository(private val context: Context) {

    private val soundsDir = File(context.filesDir, "sounds").also { it.mkdirs() }
    private val metadataFile = File(context.filesDir, "sounds_metadata.json")
    private val gson = Gson()

    private var sounds: MutableList<SoundItem> = loadSounds()

    fun getAllSounds(): List<SoundItem> = sounds.toList()

    fun addSound(title: String, sourceFile: File, sourceType: SourceType, sourceUrl: String? = null): SoundItem {
        val id = UUID.randomUUID().toString()
        val extension = sourceFile.extension.ifEmpty { "mp3" }
        val destFile = File(soundsDir, "$id.$extension")
        sourceFile.copyTo(destFile, overwrite = true)

        val sound = SoundItem(
            id = id,
            title = title,
            filePath = destFile.absolutePath,
            sourceType = sourceType,
            sourceUrl = sourceUrl
        )
        sounds.add(sound)
        saveSounds()
        return sound
    }

    fun deleteSound(sound: SoundItem) {
        File(sound.filePath).delete()
        sounds.removeAll { it.id == sound.id }
        saveSounds()
    }

    fun getSoundFile(sound: SoundItem): File = File(sound.filePath)

    private fun loadSounds(): MutableList<SoundItem> {
        if (!metadataFile.exists()) return mutableListOf()
        return try {
            val json = metadataFile.readText()
            val type = object : TypeToken<List<SoundItem>>() {}.type
            gson.fromJson<List<SoundItem>>(json, type)?.toMutableList() ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveSounds() {
        try {
            val json = gson.toJson(sounds)
            metadataFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
