package com.example.fajrapp.viewmodel

import android.app.Application
import android.icu.text.Transliterator
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.example.fajrapp.R
import com.example.fajrapp.data.LocationManager
import com.example.fajrapp.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

data class CitySuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

data class CalculationMethodOption(
    val code: String,
    val displayName: String
)

data class MadhabOption(
    val code: String,
    val displayName: String
)

data class DstModeOption(
    val code: String,
    val displayName: String
)

data class PrayerOffsetOption(
    val key: String,
    val displayName: String
)

data class SettingsUiState(
    val selectedLanguage: Language = Language("en", "English", "English", "US"),
    val locationSubtitle: String = "",
    val locationLatitude: String = "",
    val locationLongitude: String = "",
    val isUpdatingLocation: Boolean = false,
    val locationActionMessage: String? = null,
    val citySuggestions: List<CitySuggestion> = emptyList(),
    val isSearchingCities: Boolean = false,
    val selectedCalculationMethodCode: String = DEFAULT_CALC_METHOD_CODE,
    val selectedMadhabCode: String = DEFAULT_MADHAB_CODE,
    val selectedDstModeCode: String = DEFAULT_DST_MODE_CODE,
    val calculationMethodLabel: String = "",
    val madhabLabel: String = "",
    val dstModeLabel: String = "",
    val timeOffsets: Map<String, Int> = emptyMap(),
    val timeOffsetLabel: String = ""
)

private const val DEFAULT_CALC_METHOD_CODE = "MUSLIM_WORLD_LEAGUE"
private const val DEFAULT_MADHAB_CODE = "HANAFI"
private const val DEFAULT_DST_MODE_CODE = "AUTO"
private const val OFFSET_MIN = -180
private const val OFFSET_MAX = 180

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PreferencesManager(application)
    private val locationManager = LocationManager(application)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var citySearchJob: Job? = null

    val availableLanguages = listOf(
        Language("en", "English", "English", "US"),
        Language("ru", "Russian", "Русский", "RU"),
        Language("kk", "Kazakh", "Қазақша", "KZ"),
        Language("es", "Spanish", "Español", "ES"),
        Language("ar", "Arabic", "العربية", "SA"),
        Language("in", "Indonesian", "Bahasa Indonesia", "ID"),
        Language("ms", "Malaysian", "Bahasa Melayu", "MY"),
        Language("ur", "Urdu", "اردو", "PK"),
        Language("hi", "Hindi", "हिन्दी", "IN"),
        Language("uz", "Uzbek", "O'zbek", "UZ"),
        Language("ky", "Kyrgyz", "Кыргызча", "KG"),
        Language("tt", "Tatar", "Татарча", "RU"),
        Language("fa", "Farsi", "فارسی", "IR"),
        Language("tg", "Tajik", "Тоҷикӣ", "TJ"),
        Language("fr", "French", "Français", "FR")
    ).sortedBy { it.nativeName }

    val calculationMethodOptions by lazy {
        listOf(
            CalculationMethodOption("MUSLIM_WORLD_LEAGUE", getString(R.string.calc_method_mwl)),
            CalculationMethodOption("NORTH_AMERICA", getString(R.string.calc_method_isna)),
            CalculationMethodOption("EGYPTIAN", getString(R.string.calc_method_egyptian)),
            CalculationMethodOption("KARACHI", getString(R.string.calc_method_karachi)),
            CalculationMethodOption("UMM_AL_QURA", getString(R.string.calc_method_umm_al_qura)),
            CalculationMethodOption("DUBAI", getString(R.string.calc_method_dubai)),
            CalculationMethodOption("MOON_SIGHTING_COMMITTEE", getString(R.string.calc_method_moonsighting)),
            CalculationMethodOption("KUWAIT", getString(R.string.calc_method_kuwait)),
            CalculationMethodOption("QATAR", getString(R.string.calc_method_qatar)),
            CalculationMethodOption("SINGAPORE", getString(R.string.calc_method_singapore))
        )
    }

    val madhabOptions by lazy {
        listOf(
            MadhabOption("SHAFI", getString(R.string.calc_asr_shafi)),
            MadhabOption("HANAFI", getString(R.string.calc_asr_hanafi))
        )
    }

    val dstModeOptions by lazy {
        listOf(
            DstModeOption(DST_MODE_AUTO, getString(R.string.calc_dst_auto)),
            DstModeOption(DST_MODE_MINUS_ONE_HOUR, getString(R.string.calc_dst_minus_one)),
            DstModeOption(DST_MODE_PLUS_ONE_HOUR, getString(R.string.calc_dst_plus_one))
        )
    }

    val prayerOffsetOptions by lazy {
        listOf(
            PrayerOffsetOption(OFFSET_FAJR, getString(R.string.prayer_fajr)),
            PrayerOffsetOption(OFFSET_SUNRISE, getString(R.string.prayer_sunrise)),
            PrayerOffsetOption(OFFSET_DHUHR, getString(R.string.prayer_dhuhr)),
            PrayerOffsetOption(OFFSET_ASR, getString(R.string.prayer_asr)),
            PrayerOffsetOption(OFFSET_MAGHRIB, getString(R.string.prayer_maghrib)),
            PrayerOffsetOption(OFFSET_ISHA, getString(R.string.prayer_isha))
        )
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val savedLangCode = prefsManager.getLanguage() ?: "en"
        val selectedLanguage = availableLanguages.find { it.code == savedLangCode } ?: availableLanguages.first()
        val savedLocation = prefsManager.getSavedLocation()
        val selectedCalcCode = sanitizeMethodCode(prefsManager.getCalculationMethod())
        val selectedMadhabCode = sanitizeMadhabCode(prefsManager.getMadhab())
        val selectedDstCode = normalizeDstModeCode(prefsManager.getDstMode())
        val offsets = prefsManager.getPrayerOffsets(PRAYER_OFFSET_KEYS)

        _uiState.value = _uiState.value.copy(
            selectedLanguage = selectedLanguage,
            locationSubtitle = savedLocation?.cityName ?: getString(R.string.settings_location_unknown),
            locationLatitude = savedLocation?.latitude?.let(::formatCoordinate) ?: "",
            locationLongitude = savedLocation?.longitude?.let(::formatCoordinate) ?: "",
            selectedCalculationMethodCode = selectedCalcCode,
            selectedMadhabCode = selectedMadhabCode,
            selectedDstModeCode = selectedDstCode,
            calculationMethodLabel = labelForMethod(selectedCalcCode),
            madhabLabel = labelForMadhab(selectedMadhabCode),
            dstModeLabel = labelForDstMode(selectedDstCode),
            timeOffsets = offsets,
            timeOffsetLabel = buildTimeOffsetLabel(offsets)
        )
    }

    fun setLanguage(language: Language) {
        prefsManager.saveLanguage(language.code)
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun clearLocationMessage() {
        _uiState.value = _uiState.value.copy(locationActionMessage = null)
    }

    fun onCityQueryChanged(query: String) {
        clearLocationMessage()
        citySearchJob?.cancel()

        if (query.trim().length < 2) {
            _uiState.value = _uiState.value.copy(
                citySuggestions = emptyList(),
                isSearchingCities = false
            )
            return
        }

        citySearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingCities = true)
            delay(350L)
            val suggestions = searchCitySuggestions(query.trim())
            _uiState.value = _uiState.value.copy(
                citySuggestions = suggestions,
                isSearchingCities = false
            )
        }
    }

    fun selectCitySuggestion(suggestion: CitySuggestion) {
        saveResolvedLocation(suggestion.latitude, suggestion.longitude, suggestion.displayName)
        _uiState.value = _uiState.value.copy(
            citySuggestions = emptyList(),
            locationActionMessage = getString(R.string.location_saved_success)
        )
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
                locationActionMessage = getString(R.string.location_saved_success),
                citySuggestions = emptyList()
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

    fun setCalculationMethod(code: String) {
        val normalized = sanitizeMethodCode(code)
        prefsManager.saveCalculationMethod(normalized)
        _uiState.value = _uiState.value.copy(
            selectedCalculationMethodCode = normalized,
            calculationMethodLabel = labelForMethod(normalized)
        )
    }

    fun setMadhab(code: String) {
        val normalized = sanitizeMadhabCode(code)
        prefsManager.saveMadhab(normalized)
        _uiState.value = _uiState.value.copy(
            selectedMadhabCode = normalized,
            madhabLabel = labelForMadhab(normalized)
        )
    }

    fun setDstMode(code: String) {
        val normalized = normalizeDstModeCode(code)
        prefsManager.saveDstMode(normalized)
        _uiState.value = _uiState.value.copy(
            selectedDstModeCode = normalized,
            dstModeLabel = labelForDstMode(normalized)
        )
    }

    fun getTimeOffset(prayerKey: String): Int {
        return _uiState.value.timeOffsets[prayerKey] ?: 0
    }

    fun adjustTimeOffset(prayerKey: String, deltaMinutes: Int) {
        if (prayerKey !in PRAYER_OFFSET_KEYS) return

        val current = getTimeOffset(prayerKey)
        val updated = (current + deltaMinutes).coerceIn(OFFSET_MIN, OFFSET_MAX)
        prefsManager.savePrayerOffset(prayerKey, updated)

        val newOffsets = _uiState.value.timeOffsets.toMutableMap().apply {
            put(prayerKey, updated)
        }
        _uiState.value = _uiState.value.copy(
            timeOffsets = newOffsets,
            timeOffsetLabel = buildTimeOffsetLabel(newOffsets)
        )
    }

    private fun buildTimeOffsetLabel(offsets: Map<String, Int>): String {
        val hasCustomOffsets = PRAYER_OFFSET_KEYS.any { key -> (offsets[key] ?: 0) != 0 }
        return if (hasCustomOffsets) {
            getString(R.string.settings_offset_custom)
        } else {
            getString(R.string.settings_offset_default)
        }
    }

    private suspend fun searchCitySuggestions(query: String): List<CitySuggestion> {
        return withContext(Dispatchers.IO) {
            val queryCandidates = buildCityQueryCandidates(query)
            val localesToTry = listOf(Locale.getDefault(), Locale.ENGLISH, Locale("ru"), Locale("kk"))
                .distinctBy { it.toLanguageTag() }

            val suggestions = linkedMapOf<String, CitySuggestion>()

            for (locale in localesToTry) {
                try {
                    val geocoder = Geocoder(getApplication(), locale)
                    for (candidate in queryCandidates) {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(candidate, 6)
                        for (address in addresses.orEmpty()) {
                            val suggestion = citySuggestionFromAddress(address)
                            val key = "${formatCoordinate(suggestion.latitude)}|${formatCoordinate(suggestion.longitude)}"
                            suggestions.putIfAbsent(key, suggestion)
                            if (suggestions.size >= 8) {
                                return@withContext suggestions.values.toList()
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Try next locale.
                }
            }

            suggestions.values.toList()
        }
    }

    private suspend fun findByCityName(cityQuery: String): Address? {
        return withContext(Dispatchers.IO) {
            val queryCandidates = buildCityQueryCandidates(cityQuery)
            val localesToTry = listOf(Locale.getDefault(), Locale.ENGLISH, Locale("ru"), Locale("kk"))
                .distinctBy { it.toLanguageTag() }

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
        candidates += latin.replace("’", "'")
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

    private fun citySuggestionFromAddress(address: Address): CitySuggestion {
        val lat = address.latitude
        val lon = address.longitude
        val name = buildLocationDisplayName(address, lat, lon)
        return CitySuggestion(name, lat, lon)
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

    private fun sanitizeMethodCode(code: String?): String {
        if (code.isNullOrBlank()) return DEFAULT_CALC_METHOD_CODE
        return if (calculationMethodOptions.any { it.code == code }) code else DEFAULT_CALC_METHOD_CODE
    }

    private fun sanitizeMadhabCode(code: String?): String {
        if (code.isNullOrBlank()) return DEFAULT_MADHAB_CODE
        return if (madhabOptions.any { it.code == code }) code else DEFAULT_MADHAB_CODE
    }

    private fun labelForMethod(code: String): String {
        return calculationMethodOptions.find { it.code == code }?.displayName ?: getString(R.string.calc_method_mwl)
    }

    private fun labelForMadhab(code: String): String {
        return madhabOptions.find { it.code == code }?.displayName ?: getString(R.string.calc_asr_hanafi)
    }

    private fun labelForDstMode(code: String): String {
        return dstModeOptions.find { it.code == code }?.displayName ?: getString(R.string.calc_dst_auto)
    }

    private fun normalizeDstModeCode(code: String?): String {
        return normalizeDstMode(code, DEFAULT_DST_MODE_CODE)
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    companion object {
        const val DST_MODE_AUTO = "AUTO"
        const val DST_MODE_MINUS_ONE_HOUR = "MINUS_ONE_HOUR"
        const val DST_MODE_PLUS_ONE_HOUR = "PLUS_ONE_HOUR"

        const val OFFSET_FAJR = "fajr"
        const val OFFSET_SUNRISE = "sunrise"
        const val OFFSET_DHUHR = "dhuhr"
        const val OFFSET_ASR = "asr"
        const val OFFSET_MAGHRIB = "maghrib"
        const val OFFSET_ISHA = "isha"
        val PRAYER_OFFSET_KEYS = listOf(
            OFFSET_FAJR,
            OFFSET_SUNRISE,
            OFFSET_DHUHR,
            OFFSET_ASR,
            OFFSET_MAGHRIB,
            OFFSET_ISHA
        )

        fun toCalculationMethod(code: String?): CalculationMethod {
            return when (code) {
                "EGYPTIAN" -> CalculationMethod.EGYPTIAN
                "KARACHI" -> CalculationMethod.KARACHI
                "UMM_AL_QURA" -> CalculationMethod.UMM_AL_QURA
                "DUBAI" -> CalculationMethod.DUBAI
                "MOON_SIGHTING_COMMITTEE" -> CalculationMethod.MOON_SIGHTING_COMMITTEE
                "NORTH_AMERICA" -> CalculationMethod.NORTH_AMERICA
                "KUWAIT" -> CalculationMethod.KUWAIT
                "QATAR" -> CalculationMethod.QATAR
                "SINGAPORE" -> CalculationMethod.SINGAPORE
                else -> CalculationMethod.MUSLIM_WORLD_LEAGUE
            }
        }

        fun toMadhab(code: String?): Madhab {
            return if (code == "SHAFI") Madhab.SHAFI else Madhab.HANAFI
        }

        fun normalizeDstMode(code: String?, fallback: String = DST_MODE_AUTO): String {
            return when (code) {
                DST_MODE_AUTO,
                DST_MODE_MINUS_ONE_HOUR,
                DST_MODE_PLUS_ONE_HOUR -> code
                else -> fallback
            }
        }
    }
}
