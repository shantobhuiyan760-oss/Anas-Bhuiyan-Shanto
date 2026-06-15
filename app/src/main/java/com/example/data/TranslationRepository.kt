package com.example.data

import kotlinx.coroutines.flow.Flow

class TranslationRepository(private val translationDao: TranslationDao) {
    val allHistory: Flow<List<TranslationItem>> = translationDao.getAllTranslations()

    suspend fun insert(item: TranslationItem) {
        translationDao.insertTranslation(item)
    }

    suspend fun delete(id: Int) {
        translationDao.deleteTranslation(id)
    }

    suspend fun clearAll() {
        translationDao.clearAllHistory()
    }
}
