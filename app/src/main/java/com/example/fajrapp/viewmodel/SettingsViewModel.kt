package com.example.fajrapp.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fajrapp.R
import com.example.fajrapp.data.LocationManager
import com.example.fajrapp.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String
)

data class SettingsUiState(
    val selectedLanguage: Language = Language("en", "English", "English", "🇺🇸"),
    val locationSubtitle: String = "",
    val isUpdatingLocation: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PreferencesManager(application)
    private val locationManager = LocationManager(application)
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
        Language("tt", "Tatar", "Татарча", "🇷🇺"),
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
        val savedLocation = prefsManager.getSavedLocation()
        val locationText = savedLocation?.cityName ?: getString(R.string.settings_location_unknown)

        _uiState.value = _uiState.value.copy(
            selectedLanguage = lang,
            locationSubtitle = locationText
        )
    }

    fun setLanguage(language: Language) {
        prefsManager.saveLanguage(language.code)
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun updateLocationFromDevice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingLocation = true)

            val location = locationManager.getCurrentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(isUpdatingLocation = false)
                return@launch
            }

            val locationName = resolveLocationName(location.latitude, location.longitude)
            prefsManager.saveLocation(location.latitude, location.longitude, locationName)

            _uiState.value = _uiState.value.copy(
                locationSubtitle = locationName,
                isUpdatingLocation = false
            )
        }
    }

    private suspend fun resolveLocationName(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)

                if (!addresses.isNullOrEmpty()) {
                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    val country = addresses[0].countryName ?: ""
                    if (city.isNotEmpty()) "$city, $country" else country
                } else {
                    getString(R.string.settings_location_unknown)
                }
            } catch (_: Exception) {
                getString(R.string.settings_location_unknown)
            }
        }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}
