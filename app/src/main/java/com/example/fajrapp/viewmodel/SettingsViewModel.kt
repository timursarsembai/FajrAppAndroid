package com.example.fajrapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.fajrapp.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String
)

data class SettingsUiState(
    val selectedLanguage: Language = Language("en", "English", "English", "🇺🇸")
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PreferencesManager(application)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val availableLanguages = listOf(
        Language("en", "English", "English", "🇺🇸"),
        Language("ru", "Russian", "Русский", "🇷🇺"),
        Language("kk", "Kazakh", "Қазақша", "🇰🇿"),
        Language("es", "Spanish", "Español", "🇪🇸"),
        Language("ar", "Arabic", "العربية", "🇸🇦"),
        Language("in", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
        Language("ms", "Malaysian", "Bahasa Melayu", "🇲🇾"),
        Language("ur", "Urdu", "اردو", "🇵🇰"),
        Language("hi", "Hindi", "हिन्दी", "🇮🇳"),
        Language("uz", "Uzbek", "O'zbek", "🇺🇿"),
        Language("ky", "Kyrgyz", "Кыргызча", "🇰🇬"),
        Language("tt", "Tatar", "Татарча", "🇷🇺"), // Using RU flag as placeholder or specific if available
        Language("fa", "Farsi", "فارسی", "🇮🇷"),
        Language("tg", "Tajik", "Тоҷикӣ", "🇹🇯"),
        Language("fr", "French", "Français", "🇫🇷")
    ).sortedBy { it.nativeName }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val savedLangCode = prefsManager.getLanguage() ?: "en"
        val lang = availableLanguages.find { it.code == savedLangCode } ?: availableLanguages.first()
        _uiState.value = _uiState.value.copy(selectedLanguage = lang)
    }

    fun setLanguage(language: Language) {
        prefsManager.saveLanguage(language.code)
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }
}
