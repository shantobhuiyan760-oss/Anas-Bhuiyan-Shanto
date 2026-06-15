package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TranslationItem
import com.example.data.TranslationRepository
import com.example.network.GeminiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    TRANSLATE, THEMES, HISTORY, INSTALL_PROMPT, INSTALLING
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TranslationRepository(database.translationDao())

    // UI screen state
    val currentScreen = MutableStateFlow(AppScreen.TRANSLATE)

    // Language configuration
    val sourceLang = MutableStateFlow("Bengali")
    val targetLang = MutableStateFlow("English")

    // Translation edit state
    val inputText = MutableStateFlow("")
    val translatedText = MutableStateFlow("Waiting for input...")
    val isTranslating = MutableStateFlow(false)

    // Suggestion indicators & dynamic chips
    val detectionStatus = MutableStateFlow("Bengali detected")
    val suggestedWords = MutableStateFlow(listOf("the", "and", "but"))

    // Active customization config
    val accentColorIndex = MutableStateFlow(0) // 0=Purple, 1=Blue, 2=Pink, 3=Yellow, 4=Teal
    val backgroundTypeIndex = MutableStateFlow(0) // 0=Solid, 1=Gradient, 2=Image
    val solidBgIndex = MutableStateFlow(0) // 0=Charcoal, 1=Navy, 2=Obsidian
    val fontStyleIndex = MutableStateFlow(0) // 0=Inter, 1=Roboto, 2=Playfair Display
    val activePredefinedTheme = MutableStateFlow("Midnight Pro")

    // Onboarding install simulation state
    val installProgress = MutableStateFlow(0)
    val isInstalling = MutableStateFlow(false)

    // History state reactively synced from Room SQLite database
    val historyList: StateFlow<List<TranslationItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var translateJob: Job? = null

    init {
        // Trigger auto dynamic suggestions update upon input text change
        viewModelScope.launch {
            inputText.collect { text ->
                updateDetectionAndSuggestions(text)
            }
        }
    }

    private fun updateDetectionAndSuggestions(text: String) {
        if (text.isEmpty()) {
            translatedText.value = "Waiting for input..."
            suggestedWords.value = listOf("the", "and", "but")
            return
        }

        // Auto-detect Bengali characters
        val containsBengali = text.any { it in '\u0980'..'\u09FF' }
        if (containsBengali) {
            detectionStatus.value = "Bengali detected"
            suggestedWords.value = listOf("এবং", "কিন্তু", "ধন্যবাদ")
        } else {
            detectionStatus.value = "English detected"
            suggestedWords.value = listOf("the", "and", "but", "very", "good", "today")
        }
    }

    fun onKeyPress(char: String) {
        val current = inputText.value
        inputText.value = current + char
        scheduleDebouncedTranslate()
    }

    fun onBackspace() {
        val current = inputText.value
        if (current.isNotEmpty()) {
            inputText.value = current.substring(0, current.length - 1)
            scheduleDebouncedTranslate()
        }
    }

    fun onSpace() {
        inputText.value = inputText.value + " "
        scheduleDebouncedTranslate()
    }

    fun onClearInput() {
        inputText.value = ""
        translatedText.value = "Waiting for input..."
    }

    fun swapLanguages() {
        val temp = sourceLang.value
        sourceLang.value = targetLang.value
        targetLang.value = temp
        
        // Instant re-trigger translate
        scheduleDebouncedTranslate()
    }

    fun selectLanguages(from: String, to: String) {
        sourceLang.value = from
        targetLang.value = to
        scheduleDebouncedTranslate()
    }

    private fun scheduleDebouncedTranslate() {
        translateJob?.cancel()
        val text = inputText.value
        if (text.trim().isEmpty()) {
            translatedText.value = "Waiting for input..."
            return
        }

        translateJob = viewModelScope.launch {
            delay(600) // Debounce delay
            isTranslating.value = true
            
            try {
                val response = GeminiService.translateText(text, sourceLang.value, targetLang.value)
                translatedText.value = response
                
                // Automatically save full successful translations to history list!
                if (response.isNotEmpty() && !response.startsWith("Error:") && !response.startsWith("Connection error:")) {
                    repository.insert(
                        TranslationItem(
                            sourceText = text,
                            translatedText = response,
                            fromLanguage = sourceLang.value,
                            toLanguage = targetLang.value
                        )
                    )
                }
            } catch (e: Exception) {
                translatedText.value = "Translate error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isTranslating.value = false
            }
        }
    }

    fun fixGrammar() {
        val text = inputText.value
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            isTranslating.value = true
            try {
                val clean = GeminiService.fixGrammar(text, sourceLang.value)
                inputText.value = clean
                scheduleDebouncedTranslate()
            } catch (e: Exception) {
                translatedText.value = "Grammar checking failed."
            } finally {
                isTranslating.value = false
            }
        }
    }

    fun rephrase() {
        val text = inputText.value
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            isTranslating.value = true
            try {
                val rephrased = GeminiService.rephrase(text, sourceLang.value)
                translatedText.value = rephrased
                
                // Also write to Room DB!
                repository.insert(
                    TranslationItem(
                        sourceText = text,
                        translatedText = rephrased,
                        fromLanguage = sourceLang.value,
                        toLanguage = targetLang.value
                    )
                )
            } catch (e: Exception) {
                translatedText.value = "Rephrasing error."
            } finally {
                isTranslating.value = false
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun triggerInstallAppSimulation() {
        currentScreen.value = AppScreen.INSTALLING
        installProgress.value = 0
        isInstalling.value = true
        
        viewModelScope.launch {
            for (p in 1..100) {
                // Adaptive delay calculation (slowing down as it approaches 100% block for real realistic pacing)
                val delayTime = when {
                    p > 88 -> 90L
                    p > 50 -> 40L
                    else -> 20L
                }
                delay(delayTime)
                installProgress.value = p
            }
            isInstalling.value = false
            delay(600) // Brief pause at 100% Completed
            currentScreen.value = AppScreen.TRANSLATE // Bring them back on boarding!
        }
    }
}
