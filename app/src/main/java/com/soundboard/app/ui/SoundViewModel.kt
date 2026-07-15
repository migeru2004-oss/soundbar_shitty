package com.soundboard.app.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundboard.app.audio.AudioPlayer
import com.soundboard.app.audio.YouTubeDownloader
import com.soundboard.app.data.SoundRepository
import com.soundboard.app.model.SoundItem
import com.soundboard.app.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SoundViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SoundRepository(application)
    val audioPlayer = AudioPlayer(application)

    private val _sounds = MutableStateFlow<List<SoundItem>>(emptyList())
    val sounds: StateFlow<List<SoundItem>> = _sounds.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    init {
        _sounds.value = repository.getAllSounds()
    }

    fun addSoundFromFile(uri: Uri, title: String, onComplete: (SoundItem) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val sound = repository.addSound(title, tempFile, SourceType.FILE)
                _sounds.value = repository.getAllSounds()
                withContext(Dispatchers.Main) {
                    onComplete(sound)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadFromYoutube(
        url: String,
        onComplete: (SoundItem) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f

            val downloader = YouTubeDownloader(getApplication())
            val result = downloader.downloadAudio(url) { progress ->
                _downloadProgress.value = progress
            }

            withContext(Dispatchers.Main) {
                result.onSuccess { downloadResult ->
                    val sound = repository.addSound(downloadResult.title, downloadResult.file, SourceType.YOUTUBE, url)
                    _sounds.value = repository.getAllSounds()
                    _isDownloading.value = false
                    onComplete(sound)
                }.onFailure { error ->
                    _isDownloading.value = false
                    onError(error.message ?: "Download failed")
                }
            }
        }
    }

    fun playSound(sound: SoundItem) {
        audioPlayer.play(sound)
    }

    fun deleteSound(sound: SoundItem) {
        repository.deleteSound(sound)
        _sounds.value = repository.getAllSounds()
    }

    fun getFileName(uri: Uri): String? {
        val context = getApplication<Application>()
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            if (nameIndex >= 0) it.getString(nameIndex) else null
        } ?: uri.lastPathSegment
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
