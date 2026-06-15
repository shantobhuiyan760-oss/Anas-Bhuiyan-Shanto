package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceText: String,
    val translatedText: String,
    val fromLanguage: String,
    val toLanguage: String,
    val timestamp: Long = System.currentTimeMillis()
)
