package com.example.fajrapp.viewmodel

import android.app.Application
import android.content.Context
import android.icu.text.Transliterator
import android.location.Address
import android.location.Geocoder
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Madhab
import com.example.fajrapp.FajrApp
import com.example.fajrapp.R
import com.example.fajrapp.data.LocationManager
import com.example.fajrapp.data.PreferencesManager
import com.example.fajrapp.util.LocationNameLocalizer
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
    val rawName: String,
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

data class NotificationSoundOption(
    val uri: String?,
    val title: String,
    val sourceKey: String
)

data class PrayerNotificationOption(
    val prayerKey: String,
    val prayerLabel: String,
    val enabled: Boolean,
    val soundUri: String?,
    val soundTitle: String
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
    val notificationsSummary: String = "",
    val notificationsUseSingleSound: Boolean = true,
    val notificationsGlobalSoundUri: String? = null,
    val notificationsGlobalSoundTitle: String = "",
    val notificationsByPrayer: List<PrayerNotificationOption> = emptyList(),
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
    private var cachedLocaleCode: String? = null
    private var calculationMethodOptionsCache: List<CalculationMethodOption> = emptyList()
    private var madhabOptionsCache: List<MadhabOption> = emptyList()
    private var dstModeOptionsCache: List<DstModeOption> = emptyList()
    private var prayerOffsetOptionsCache: List<PrayerOffsetOption> = emptyList()
    private var notificationPrayerDefinitionsCache: List<Pair<String, String>> = emptyList()
    private var notificationSoundOptionsCache: List<NotificationSoundOption> = emptyList()

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

    val calculationMethodOptions: List<CalculationMethodOption>
        get() {
            ensureLocalizedCaches()
            return calculationMethodOptionsCache
        }

    val madhabOptions: List<MadhabOption>
        get() {
            ensureLocalizedCaches()
            return madhabOptionsCache
        }

    val dstModeOptions: List<DstModeOption>
        get() {
            ensureLocalizedCaches()
            return dstModeOptionsCache
        }

    val prayerOffsetOptions: List<PrayerOffsetOption>
        get() {
            ensureLocalizedCaches()
            return prayerOffsetOptionsCache
        }

    val notificationSoundOptions: List<NotificationSoundOption>
        get() {
            ensureLocalizedCaches()
            return notificationSoundOptionsCache
        }

    fun notificationSoundOptionsForTarget(targetKey: String): List<NotificationSoundOption> {
        ensureLocalizedCaches()
        return when (targetKey) {
            OFFSET_FAJR -> notificationSoundOptionsCache
            NOTIFICATION_TARGET_GLOBAL -> notificationSoundOptionsCache
            else -> notificationSoundOptionsCache.filter { it.sourceKey != SOUND_SOURCE_AZAN_FAJR }
        }
    }

    init {
        loadSettings()
    }

    private fun ensureLocalizedCaches() {
        val localeCode = appLanguageCode()
        if (cachedLocaleCode == localeCode && calculationMethodOptionsCache.isNotEmpty()) return

        cachedLocaleCode = localeCode
        calculationMethodOptionsCache = listOf(
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
        madhabOptionsCache = listOf(
            MadhabOption("SHAFI", getString(R.string.calc_asr_shafi)),
            MadhabOption("HANAFI", getString(R.string.calc_asr_hanafi))
        )
        dstModeOptionsCache = listOf(
            DstModeOption(DST_MODE_AUTO, getString(R.string.calc_dst_auto)),
            DstModeOption(DST_MODE_MINUS_ONE_HOUR, getString(R.string.calc_dst_minus_one)),
            DstModeOption(DST_MODE_PLUS_ONE_HOUR, getString(R.string.calc_dst_plus_one))
        )
        prayerOffsetOptionsCache = listOf(
            PrayerOffsetOption(OFFSET_FAJR, getString(R.string.prayer_fajr)),
            PrayerOffsetOption(OFFSET_SUNRISE, getString(R.string.prayer_sunrise)),
            PrayerOffsetOption(OFFSET_DHUHR, getString(R.string.prayer_dhuhr)),
            PrayerOffsetOption(OFFSET_ASR, getString(R.string.prayer_asr)),
            PrayerOffsetOption(OFFSET_MAGHRIB, getString(R.string.prayer_maghrib)),
            PrayerOffsetOption(OFFSET_ISHA, getString(R.string.prayer_isha))
        )
        notificationPrayerDefinitionsCache = listOf(
            OFFSET_FAJR to getString(R.string.prayer_fajr),
            PRAYER_DUHA to getString(R.string.prayer_sunrise),
            OFFSET_DHUHR to getString(R.string.prayer_dhuhr),
            OFFSET_ASR to getString(R.string.prayer_asr),
            OFFSET_MAGHRIB to getString(R.string.prayer_maghrib),
            OFFSET_ISHA to getString(R.string.prayer_isha),
            PRAYER_TAHAJJUD to getString(R.string.prayer_tahajjud)
        )
        notificationSoundOptionsCache = buildNotificationSoundOptions()
    }

    private fun notificationPrayerDefinitions(): List<Pair<String, String>> {
        ensureLocalizedCaches()
        return notificationPrayerDefinitionsCache
    }

    private fun loadSettings() {
        ensureLocalizedCaches()
        val savedLangCode = normalizeAppLanguageCode(
            FajrApp.ensureAppLanguagePreference(getApplication())
        )
        val selectedLanguage = availableLanguages.find { it.code == savedLangCode } ?: availableLanguages.first()
        val savedLocation = prefsManager.getSavedLocation()
        val selectedCalcCode = sanitizeMethodCode(prefsManager.getCalculationMethod())
        val selectedMadhabCode = sanitizeMadhabCode(prefsManager.getMadhab())
        val selectedDstCode = normalizeDstModeCode(prefsManager.getDstMode())
        val offsets = prefsManager.getPrayerOffsets(PRAYER_OFFSET_KEYS)
        val notificationsUseSingleSound = prefsManager.getNotificationsUseSingleSound()
        val notificationsGlobalSoundUri = prefsManager.getNotificationsGlobalSoundUri()
        val notificationsGlobalSoundTitle = resolveSoundTitle(
            uri = notificationsGlobalSoundUri,
            storedTitle = prefsManager.getNotificationsGlobalSoundTitle()
        )
        val notificationsByPrayer = notificationPrayerDefinitions().map { (prayerKey, prayerLabel) ->
            val soundUri = prefsManager.getPrayerNotificationSoundUri(prayerKey)
            val soundTitle = resolveSoundTitle(
                uri = soundUri,
                storedTitle = prefsManager.getPrayerNotificationSoundTitle(prayerKey)
            )
            PrayerNotificationOption(
                prayerKey = prayerKey,
                prayerLabel = prayerLabel,
                enabled = prefsManager.getPrayerNotificationEnabled(prayerKey),
                soundUri = soundUri,
                soundTitle = soundTitle
            )
        }

        _uiState.value = _uiState.value.copy(
            selectedLanguage = selectedLanguage,
            locationSubtitle = savedLocation?.cityName?.let(::localizeLocationName)
                ?: getString(R.string.settings_location_unknown),
            locationLatitude = savedLocation?.latitude?.let(::formatCoordinate) ?: "",
            locationLongitude = savedLocation?.longitude?.let(::formatCoordinate) ?: "",
            selectedCalculationMethodCode = selectedCalcCode,
            selectedMadhabCode = selectedMadhabCode,
            selectedDstModeCode = selectedDstCode,
            calculationMethodLabel = labelForMethod(selectedCalcCode),
            madhabLabel = labelForMadhab(selectedMadhabCode),
            dstModeLabel = labelForDstMode(selectedDstCode),
            notificationsSummary = buildNotificationsSummary(
                useSingleSound = notificationsUseSingleSound,
                prayerOptions = notificationsByPrayer
            ),
            notificationsUseSingleSound = notificationsUseSingleSound,
            notificationsGlobalSoundUri = notificationsGlobalSoundUri,
            notificationsGlobalSoundTitle = notificationsGlobalSoundTitle,
            notificationsByPrayer = notificationsByPrayer,
            timeOffsets = offsets,
            timeOffsetLabel = buildTimeOffsetLabel(offsets)
        )
    }

    fun setLanguage(language: Language) {
        val normalized = normalizeAppLanguageCode(language.code)
        prefsManager.saveLanguage(normalized)
        cachedLocaleCode = null
        loadSettings()
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
        saveResolvedLocation(suggestion.latitude, suggestion.longitude, suggestion.rawName)
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

    fun setNotificationsUseSingleSound(enabled: Boolean) {
        prefsManager.saveNotificationsUseSingleSound(enabled)
        loadSettings()
    }

    fun setGlobalNotificationSound(option: NotificationSoundOption) {
        prefsManager.saveNotificationsGlobalSound(option.uri, option.title)
        loadSettings()
    }

    fun setPrayerNotificationEnabled(prayerKey: String, enabled: Boolean) {
        if (prayerKey !in NOTIFICATION_PRAYER_KEYS) return
        prefsManager.savePrayerNotificationEnabled(prayerKey, enabled)
        loadSettings()
    }

    fun setPrayerNotificationSound(prayerKey: String, option: NotificationSoundOption) {
        if (prayerKey !in NOTIFICATION_PRAYER_KEYS) return
        prefsManager.savePrayerNotificationSound(prayerKey, option.uri, option.title)
        loadSettings()
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

    private fun buildNotificationsSummary(
        useSingleSound: Boolean,
        prayerOptions: List<PrayerNotificationOption>
    ): String {
        if (useSingleSound) return getString(R.string.settings_notifications_on)
        val enabledCount = prayerOptions.count { it.enabled }
        return getString(R.string.settings_notifications_enabled_count, enabledCount, prayerOptions.size)
    }

    private fun resolveSoundTitle(uri: String?, storedTitle: String): String {
        if (uri == null) return getString(R.string.notification_sound_system_default)
        notificationSoundOptions.firstOrNull { it.uri == uri }?.title?.let { resolved ->
            if (resolved.isNotBlank()) return resolved
        }
        if (storedTitle.isNotBlank()) return storedTitle
        return getString(R.string.notification_sound_system_default)
    }

    private fun buildNotificationSoundOptions(): List<NotificationSoundOption> {
        val app = getApplication<Application>()
        val result = mutableListOf(
            NotificationSoundOption(
                uri = null,
                title = getString(R.string.notification_sound_system_default),
                sourceKey = SOUND_SOURCE_SYSTEM
            )
        )

        addAzanAssetOptions(
            result = result,
            directory = "audio/azan",
            sourceKey = SOUND_SOURCE_AZAN
        )
        addAzanAssetOptions(
            result = result,
            directory = "audio/azan/fajr",
            sourceKey = SOUND_SOURCE_AZAN_FAJR
        )

        val manager = RingtoneManager(app).apply {
            setType(RingtoneManager.TYPE_ALARM)
        }
        val cursor = manager.cursor
        cursor?.use {
            val seenUris = result.mapNotNull { option -> option.uri }.toMutableSet()
            while (it.moveToNext()) {
                val uri = manager.getRingtoneUri(it.position)?.toString() ?: continue
                if (!seenUris.add(uri)) continue
                val title = it.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty().ifBlank {
                    getString(R.string.notification_sound_system_default)
                }
                result.add(
                    NotificationSoundOption(
                        uri = uri,
                        title = title,
                        sourceKey = SOUND_SOURCE_SYSTEM
                    )
                )
            }
        }

        return result
    }

    private fun addAzanAssetOptions(
        result: MutableList<NotificationSoundOption>,
        directory: String,
        sourceKey: String
    ) {
        val app = getApplication<Application>()
        try {
            val files = app.assets.list(directory).orEmpty()
                .filter { name ->
                    val lower = name.lowercase(Locale.US)
                    lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a")
                }
                .sortedBy { it.lowercase(Locale.US) }

            files.forEach { fileName ->
                result.add(
                    NotificationSoundOption(
                        uri = "asset://$directory/$fileName",
                        title = prettifyAssetName(fileName),
                        sourceKey = sourceKey
                    )
                )
            }
        } catch (_: Exception) {
            // Ignore assets read errors.
        }
    }

    private fun prettifyAssetName(fileName: String): String {
        val baseName = fileName.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        if (baseName.isBlank()) return fileName
        return baseName.split(Regex("\\s+"))
            .joinToString(" ") { part ->
                part.replaceFirstChar { c ->
                    if (c.isLowerCase()) c.titlecase(currentAppLocale()) else c.toString()
                }
            }
    }

    private suspend fun searchCitySuggestions(query: String): List<CitySuggestion> {
        return withContext(Dispatchers.IO) {
            val queryCandidates = buildCityQueryCandidates(query)
            val localesToTry = listOf(currentAppLocale(), Locale.ENGLISH, Locale("ru"), Locale("kk"))
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
            val localesToTry = listOf(currentAppLocale(), Locale.ENGLISH, Locale("ru"), Locale("kk"))
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
                val geocoder = Geocoder(getApplication(), currentAppLocale())
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
        val rawName = buildLocationDisplayName(address, lat, lon)
        val displayName = localizeLocationName(rawName)
        return CitySuggestion(displayName, rawName, lat, lon)
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
            locationSubtitle = localizeLocationName(locationName),
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
        return getLocalizedContext().getString(resId)
    }

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return getLocalizedContext().getString(resId, *formatArgs)
    }

    private fun normalizeAppLanguageCode(code: String): String {
        return when (code.lowercase(Locale.US)) {
            "kz" -> "kk"
            "id" -> "in"
            else -> code.lowercase(Locale.US)
        }
    }

    private fun localizeLocationName(rawName: String): String {
        return LocationNameLocalizer.localizeForUi(rawName, currentAppLocale())
    }

    private fun appLanguageCode(): String {
        val raw = FajrApp.ensureAppLanguagePreference(getApplication())
        return normalizeAppLanguageCode(raw)
    }

    private fun currentAppLocale(): Locale {
        return Locale(appLanguageCode())
    }

    private fun getLocalizedContext(): Context {
        return FajrApp.updateBaseContextLocale(
            getApplication<Application>(),
            appLanguageCode()
        )
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
        const val PRAYER_DUHA = "duha"
        const val PRAYER_TAHAJJUD = "tahajjud"
        const val SOUND_SOURCE_SYSTEM = "system"
        const val SOUND_SOURCE_AZAN = "azan"
        const val SOUND_SOURCE_AZAN_FAJR = "azan_fajr"
        const val NOTIFICATION_TARGET_GLOBAL = "global"
        val PRAYER_OFFSET_KEYS = listOf(
            OFFSET_FAJR,
            OFFSET_SUNRISE,
            OFFSET_DHUHR,
            OFFSET_ASR,
            OFFSET_MAGHRIB,
            OFFSET_ISHA
        )
        val NOTIFICATION_PRAYER_KEYS = listOf(
            OFFSET_FAJR,
            PRAYER_DUHA,
            OFFSET_DHUHR,
            OFFSET_ASR,
            OFFSET_MAGHRIB,
            OFFSET_ISHA,
            PRAYER_TAHAJJUD
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
