package com.soundboard.app.model

data class SoundItem(
    val id: String,
    val title: String,
    val filePath: String,
    val sourceType: SourceType,
    val sourceUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SourceType {
    FILE, YOUTUBE
}
