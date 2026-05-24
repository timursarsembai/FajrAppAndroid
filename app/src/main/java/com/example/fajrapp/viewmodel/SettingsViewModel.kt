package com.example.fajrapp.viewmodel

import android.app.Application
import android.icu.text.Transliterator
import android.location.Address
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
    val locationLatitude: String = "",
    val locationLongitude: String = "",
    val isUpdatingLocation: Boolean = false,
    val locationActionMessage: String? = null
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
        val selectedLanguage = availableLanguages.find { it.code == savedLangCode } ?: availableLanguages.first()
        val savedLocation = prefsManager.getSavedLocation()

        _uiState.value = _uiState.value.copy(
            selectedLanguage = selectedLanguage,
            locationSubtitle = savedLocation?.cityName ?: getString(R.string.settings_location_unknown),
            locationLatitude = savedLocation?.latitude?.let(::formatCoordinate) ?: "",
            locationLongitude = savedLocation?.longitude?.let(::formatCoordinate) ?: ""
        )
    }

    fun setLanguage(language: Language) {
        prefsManager.saveLanguage(language.code)
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun clearLocationMessage() {
        _uiState.value = _uiState.value.copy(locationActionMessage = null)
    }

    fun updateLocationFromDevice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingLocation = true,
                locationActionMessage = null
            )

            val location = locationManager.getCurrentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingLocation = false,
                    locationActionMessage = getString(R.string.location_error_device)
                )
                return@launch
            }

            val locationName = resolveLocationName(location.latitude, location.longitude)
            saveResolvedLocation(location.latitude, location.longitude, locationName)
            _uiState.value = _uiState.value.copy(
                isUpdatingLocation = false,
                locationActionMessage = getString(R.string.location_saved_success)
            )
        }
    }

    fun updateLocationFromCity(cityQuery: String) {
        if (cityQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(locationActionMessage = getString(R.string.location_error_empty_city))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingLocation = true,
                locationActionMessage = null
            )

            val address = findByCityName(cityQuery.trim())
            if (address == null) {
                _uiState.value = _uiState.value.copy(
                    isUpdatingLocation = false,
                    locationActionMessage = getString(R.string.location_error_city_not_found)
                )
                return@launch
            }

            val lat = address.latitude
            val lon = address.longitude
            val locationName = buildLocationDisplayName(address, lat, lon)

            saveResolvedLocation(lat, lon, locationName)
            _uiState.value = _uiState.value.copy(
                isUpdatingLocation = false,
                locationActionMessage = getString(R.string.location_saved_success)
            )
        }
    }

    fun updateLocationFromCoordinates(latitudeInput: String, longitudeInput: String) {
        val lat = parseCoordinate(latitudeInput)
        val lon = parseCoordinate(longitudeInput)

        if (lat == null || lon == null) {
            _uiState.value = _uiState.value.copy(locationActionMessage = getString(R.string.location_error_invalid_coordinates))
            return
        }
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            _uiState.value = _uiState.value.copy(locationActionMessage = getString(R.string.location_error_out_of_range))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingLocation = true,
                locationActionMessage = null
            )

            val locationName = resolveLocationName(lat, lon)
            saveResolvedLocation(lat, lon, locationName)
            _uiState.value = _uiState.value.copy(
                isUpdatingLocation = false,
                locationActionMessage = getString(R.string.location_saved_success)
            )
        }
    }

    private suspend fun findByCityName(cityQuery: String): Address? {
        return withContext(Dispatchers.IO) {
            val queryCandidates = buildCityQueryCandidates(cityQuery)
            val localesToTry = listOf(
                Locale.getDefault(),
                Locale.ENGLISH,
                Locale("ru"),
                Locale("kk")
            ).distinctBy { it.toLanguageTag() }

            for (locale in localesToTry) {
                try {
                    val geocoder = Geocoder(getApplication(), locale)
                    for (query in queryCandidates) {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(query, 3)
                        val found = addresses?.firstOrNull()
                        if (found != null) return@withContext found
                    }
                } catch (_: Exception) {
                    // Try next locale/query variation.
                }
            }
            null
        }
    }

    private fun buildCityQueryCandidates(rawQuery: String): List<String> {
        val query = rawQuery.trim().replace(Regex("\\s+"), " ")
        val candidates = linkedSetOf<String>()
        if (query.isBlank()) return emptyList()

        candidates += query
        candidates += query.replace('Ё', 'Е').replace('ё', 'е')

        val latin = transliterateToLatin(query)
        candidates += latin
        candidates += latin.replace('’', '\'')
        candidates += latin.replace("'", "")

        return candidates.filter { it.isNotBlank() }
    }

    private fun transliterateToLatin(value: String): String {
        return try {
            val transliterator = Transliterator.getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")
            transliterator.transliterate(value)
        } catch (_: Exception) {
            value
        }
    }

    private suspend fun resolveLocationName(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val firstAddress = addresses?.firstOrNull()
                buildLocationDisplayName(firstAddress, lat, lon)
            } catch (_: Exception) {
                fallbackLocationLabel(lat, lon)
            }
        }
    }

    private fun buildLocationDisplayName(address: Address?, lat: Double, lon: Double): String {
        if (address == null) return fallbackLocationLabel(lat, lon)

        val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
        val country = address.countryName ?: ""

        return when {
            city.isNotEmpty() && country.isNotEmpty() -> "$city, $country"
            city.isNotEmpty() -> city
            country.isNotEmpty() -> country
            else -> fallbackLocationLabel(lat, lon)
        }
    }

    private fun fallbackLocationLabel(lat: Double, lon: Double): String {
        return "${formatCoordinate(lat)}, ${formatCoordinate(lon)}"
    }

    private fun saveResolvedLocation(lat: Double, lon: Double, locationName: String) {
        prefsManager.saveLocation(lat, lon, locationName)
        _uiState.value = _uiState.value.copy(
            locationSubtitle = locationName,
            locationLatitude = formatCoordinate(lat),
            locationLongitude = formatCoordinate(lon)
        )
    }

    private fun parseCoordinate(value: String): Double? {
        return value.trim().replace(',', '.').toDoubleOrNull()
    }

    private fun formatCoordinate(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}
