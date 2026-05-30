package com.example.fajrapp.viewmodel

import android.app.Application
import android.icu.util.TimeZone as IcuTimeZone
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.fajrapp.FajrApp
import com.example.fajrapp.R
import com.example.fajrapp.data.LocationManager
import com.example.fajrapp.data.PreferencesManager
import com.example.fajrapp.data.SavedLocation
import com.example.fajrapp.model.PrayerData
import com.example.fajrapp.util.LocationNameLocalizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class PrayerUiState(
    val prayerTimes: List<PrayerData> = emptyList(),
    val locationName: String = "",
    val hijriDate: String = "",
    val gregorianDate: String = "",
    val isLoading: Boolean = true
)

private data class PrayerEntry(
    val key: String,
    val offsetKey: String?,
    val name: String,
    val arabicName: String,
    val time: Date
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val locationManager = LocationManager(application)
    private val prefsManager = PreferencesManager(application)
    private var nextPrayerTime: Date? = null
    private var lastConfigSignature: String? = null
    private var cachedRegionTimeZone: TimeZone? = null
    private var cachedRegionTimeZoneKey: String? = null

    // Fallback coordinates used only until a saved/device location is resolved.
    private var currentCoordinates = Coordinates(0.0, 0.0)

    init {
        _uiState.value = _uiState.value.copy(locationName = getString(R.string.str_loading))
        loadInitialData()
    }

    fun refreshForLocaleChange() {
        maybeReloadConfiguration(force = true)
    }

    fun refreshLocationFromDevice() {
        viewModelScope.launch {
            val location = locationManager.getBestAvailableLocation() ?: return@launch
            currentCoordinates = Coordinates(location.latitude, location.longitude)
            updateLocationNameAndSave(location.latitude, location.longitude)
            maybeReloadConfiguration(force = true)
        }
    }

    private fun getString(resId: Int): String {
        return getLocalizedContext().getString(resId)
    }

    private fun getString(resId: Int, vararg formatArgs: Any): String {
        return getLocalizedContext().getString(resId, *formatArgs)
    }

    private fun loadInitialData() {
        val saved = prefsManager.getSavedLocation()
        if (saved != null) {
            currentCoordinates = Coordinates(saved.latitude, saved.longitude)
            _uiState.value = _uiState.value.copy(
                locationName = localizeLocationName(saved.cityName),
                isLoading = false
            )
            maybeReloadConfiguration(force = true)
        }

        startTimer()

        viewModelScope.launch {
            try {
                val location = findBestLocationWithRetries(saved)
                if (location != null) {
                    val isDifferent = if (saved != null) {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                            saved.latitude,
                            saved.longitude,
                            location.latitude,
                            location.longitude,
                            results
                        )
                        results[0] > 1000
                    } else {
                        true
                    }

                    if (isDifferent) {
                        currentCoordinates = Coordinates(location.latitude, location.longitude)
                        updateLocationNameAndSave(location.latitude, location.longitude)
                        maybeReloadConfiguration(force = true)
                    }
                } else if (saved == null) {
                    _uiState.value = _uiState.value.copy(
                        locationName = getString(R.string.settings_location_unknown),
                        isLoading = false
                    )
                    maybeReloadConfiguration(force = true)
                }
            } catch (_: Exception) {
                if (saved == null) {
                    _uiState.value = _uiState.value.copy(
                        locationName = getString(R.string.settings_location_unknown),
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun findBestLocationWithRetries(saved: SavedLocation?): android.location.Location? {
        val attempts = if (saved == null) 5 else 2
        repeat(attempts) { attempt ->
            val resolved = locationManager.getBestAvailableLocation()
            if (resolved != null) return resolved
            if (attempt < attempts - 1) delay(1500L)
        }
        return null
    }

    private fun updateLocationNameAndSave(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(getApplication(), currentAppLocale())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)

                val rawLocationName = if (!addresses.isNullOrEmpty()) {
                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    val country = addresses[0].countryName ?: ""
                    if (city.isNotEmpty()) "$city, $country" else country
                } else {
                    getString(R.string.settings_location_unknown)
                }

            _uiState.value = _uiState.value.copy(locationName = localizeLocationName(rawLocationName))
            prefsManager.saveLocation(lat, lon, rawLocationName)
        } catch (_: Exception) {
            val fallbackName = "${formatCoordinate(lat)}, ${formatCoordinate(lon)}"
            _uiState.value = _uiState.value.copy(locationName = fallbackName)
            prefsManager.saveLocation(lat, lon, fallbackName)
        }
    }
    }

    private fun calculatePrayerTimes() {
        val today = Calendar.getInstance()
        val dateComponents = DateComponents(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH)
        )

        val method = SettingsViewModel.toCalculationMethod(prefsManager.getCalculationMethod())
        val madhab = SettingsViewModel.toMadhab(prefsManager.getMadhab())
        val params = method.parameters
        params.madhab = madhab

        val offsets = prefsManager.getPrayerOffsets(SettingsViewModel.PRAYER_OFFSET_KEYS)
        val dstShiftMinutes = getConfiguredDstShiftMinutes(Date())
        val prayerTimes = PrayerTimes(currentCoordinates, dateComponents, params)

        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        val tomorrowComponents = DateComponents(
            tomorrow.get(Calendar.YEAR),
            tomorrow.get(Calendar.MONTH) + 1,
            tomorrow.get(Calendar.DAY_OF_MONTH)
        )
        val tomorrowTimes = PrayerTimes(currentCoordinates, tomorrowComponents, params)

        val appLocale = currentAppLocale()
        val timeFormatter = SimpleDateFormat("HH:mm", appLocale)
        val now = Date()

        val sunriseOffsetMinutes = offsets[SettingsViewModel.OFFSET_SUNRISE] ?: 0
        val dhuhrOffsetMinutes = offsets[SettingsViewModel.OFFSET_DHUHR] ?: 0
        val sunriseWithOffsets = applyOffset(prayerTimes.sunrise, sunriseOffsetMinutes + dstShiftMinutes)
        val duhaTime = applyOffset(prayerTimes.sunrise, 20)
        val duhaWithOffsets = applyOffset(duhaTime, sunriseOffsetMinutes + dstShiftMinutes)
        val dhuhrWithOffsets = applyOffset(prayerTimes.dhuhr, dhuhrOffsetMinutes + dstShiftMinutes)
        val duhaRecommendedStart = applyOffset(dhuhrWithOffsets, -120)
        val duhaRecommendedEnd = applyOffset(dhuhrWithOffsets, -90)
        val duhaInfo = getString(
            R.string.prayer_duha_explanation_format,
            timeFormatter.format(sunriseWithOffsets),
            timeFormatter.format(duhaWithOffsets),
            timeFormatter.format(duhaRecommendedStart),
            timeFormatter.format(duhaRecommendedEnd)
        )
        val tahajjudTime = calculateTahajjudStart(
            ishaTime = prayerTimes.isha,
            nextFajrTime = tomorrowTimes.fajr
        )

        val entries = listOf(
            PrayerEntry("fajr", SettingsViewModel.OFFSET_FAJR, getString(R.string.prayer_fajr), "الفجر", prayerTimes.fajr),
            PrayerEntry("duha", SettingsViewModel.OFFSET_SUNRISE, getString(R.string.prayer_sunrise), "الضحى", duhaTime),
            PrayerEntry("dhuhr", SettingsViewModel.OFFSET_DHUHR, getString(R.string.prayer_dhuhr), "الظهر", prayerTimes.dhuhr),
            PrayerEntry("asr", SettingsViewModel.OFFSET_ASR, getString(R.string.prayer_asr), "العصر", prayerTimes.asr),
            PrayerEntry("maghrib", SettingsViewModel.OFFSET_MAGHRIB, getString(R.string.prayer_maghrib), "المغرب", prayerTimes.maghrib),
            PrayerEntry("isha", SettingsViewModel.OFFSET_ISHA, getString(R.string.prayer_isha), "العشاء", prayerTimes.isha),
            PrayerEntry("tahajjud", null, getString(R.string.prayer_tahajjud), "التهجد", tahajjudTime)
        ).map { entry ->
            val prayerOffset = entry.offsetKey?.let { offsets[it] ?: 0 } ?: 0
            entry.copy(time = applyOffset(entry.time, prayerOffset + dstShiftMinutes))
        }

        val prayersList = mutableListOf<PrayerData>()
        var foundNext = false

        for (entry in entries) {
            val isPassed = now.after(entry.time)
            val isNext = !isPassed && !foundNext
            if (isNext) {
                foundNext = true
                nextPrayerTime = entry.time
            }

            prayersList.add(
                PrayerData(
                    key = entry.key,
                    name = entry.name,
                    arabicName = entry.arabicName,
                    time = timeFormatter.format(entry.time),
                    timeMillis = entry.time.time,
                    extraInfo = if (entry.key == "duha") duhaInfo else null,
                    isNext = isNext,
                    isPassed = isPassed && !isNext
                )
            )
        }

        if (!foundNext && prayersList.isNotEmpty()) {
            nextPrayerTime = applyOffset(
                tomorrowTimes.fajr,
                (offsets[SettingsViewModel.OFFSET_FAJR] ?: 0) + dstShiftMinutes
            )

            prayersList[0] = prayersList[0].copy(
                isNext = true,
                isPassed = false
            )
        }

        val gregorianFormatter = SimpleDateFormat("d MMMM yyyy", appLocale)
        val gregorianDate = gregorianFormatter.format(today.time)
        val hijriDate = calculateHijriDate()

        _uiState.value = _uiState.value.copy(
            prayerTimes = prayersList,
            gregorianDate = gregorianDate,
            hijriDate = hijriDate,
            isLoading = false
        )
    }

    private fun calculateHijriDate(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hijrahDate = HijrahDate.now()
            "${hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)} " +
                "${getHijriMonthName(hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR))} " +
                hijrahDate.get(java.time.temporal.ChronoField.YEAR)
        } else {
            "15 ${getString(R.string.hijri_rajab)} 1448"
        }
    }

    private fun getHijriMonthName(month: Int): String {
        return when (month) {
            1 -> getString(R.string.hijri_muharram)
            2 -> getString(R.string.hijri_safar)
            3 -> getString(R.string.hijri_rabi_al_awwal)
            4 -> getString(R.string.hijri_rabi_al_athani)
            5 -> getString(R.string.hijri_jumada_al_ula)
            6 -> getString(R.string.hijri_jumada_al_akhirah)
            7 -> getString(R.string.hijri_rajab)
            8 -> getString(R.string.hijri_shaban)
            9 -> getString(R.string.hijri_ramadan)
            10 -> getString(R.string.hijri_shawwal)
            11 -> getString(R.string.hijri_dhu_al_qadah)
            12 -> getString(R.string.hijri_dhu_al_hijjah)
            else -> ""
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            var secondsFromLastConfigCheck = 0
            while (true) {
                val now = Date()

                if (nextPrayerTime?.before(now) == true) {
                    calculatePrayerTimes()
                    secondsFromLastConfigCheck = 0
                }

                if (secondsFromLastConfigCheck >= 30) {
                    maybeReloadConfiguration()
                    secondsFromLastConfigCheck = 0
                } else {
                    secondsFromLastConfigCheck++
                }
                delay(1000L)
            }
        }
    }

    private fun maybeReloadConfiguration(force: Boolean = false) {
        val saved = prefsManager.getSavedLocation()
        val lat = saved?.latitude?.toString() ?: currentCoordinates.latitude.toString()
        val lon = saved?.longitude?.toString() ?: currentCoordinates.longitude.toString()
        val methodCode = prefsManager.getCalculationMethod() ?: "MUSLIM_WORLD_LEAGUE"
        val madhabCode = prefsManager.getMadhab() ?: "HANAFI"
        val dstModeCode = SettingsViewModel.normalizeDstMode(prefsManager.getDstMode())
        val languageCode = appLanguageCode()
        val dstShift = getConfiguredDstShiftMinutes(Date())
        val offsetSignature = SettingsViewModel.PRAYER_OFFSET_KEYS.joinToString("|") {
            prefsManager.getPrayerOffset(it).toString()
        }
        val signature = "$lat|$lon|$methodCode|$madhabCode|$dstModeCode|$languageCode|$dstShift|$offsetSignature"

        if (!force && signature == lastConfigSignature) return

        saved?.let {
            currentCoordinates = Coordinates(it.latitude, it.longitude)
            _uiState.value = _uiState.value.copy(locationName = localizeLocationName(it.cityName))
        }

        lastConfigSignature = signature
        calculatePrayerTimes()
    }

    private fun applyOffset(source: Date, offsetMinutes: Int): Date {
        return Date(source.time + offsetMinutes * 60_000L)
    }

    private fun calculateTahajjudStart(ishaTime: Date, nextFajrTime: Date): Date {
        var nightDuration = nextFajrTime.time - ishaTime.time
        if (nightDuration <= 0L) {
            nightDuration += TimeUnit.DAYS.toMillis(1)
        }
        val thirdPartStartOffset = (nightDuration * 2L) / 3L
        return Date(ishaTime.time + thirdPartStartOffset)
    }

    private fun getConfiguredDstShiftMinutes(now: Date): Int {
        return when (SettingsViewModel.normalizeDstMode(prefsManager.getDstMode())) {
            SettingsViewModel.DST_MODE_MINUS_ONE_HOUR -> -60
            SettingsViewModel.DST_MODE_PLUS_ONE_HOUR -> 60
            else -> getAutoDstShiftMinutes(now)
        }
    }

    private fun getAutoDstShiftMinutes(now: Date): Int {
        val timeZone = resolveRegionTimeZone() ?: return 0
        return if (timeZone.useDaylightTime() && timeZone.inDaylightTime(now)) 60 else 0
    }

    private fun resolveRegionTimeZone(): TimeZone? {
        val saved = prefsManager.getSavedLocation()
        val locationKey = buildLocationKeyForTimeZone(saved)
        if (cachedRegionTimeZone != null && cachedRegionTimeZoneKey == locationKey) {
            return cachedRegionTimeZone
        }

        val countryCode = resolveCountryCode(saved?.cityName)
        val candidateIds = getTimeZoneCandidates(countryCode)
        val timeZoneId = selectBestTimeZoneId(
            candidateIds = candidateIds,
            cityLabel = saved?.cityName.orEmpty(),
            longitude = saved?.longitude ?: currentCoordinates.longitude
        ) ?: TimeZone.getDefault().id

        return TimeZone.getTimeZone(timeZoneId).also {
            cachedRegionTimeZone = it
            cachedRegionTimeZoneKey = locationKey
        }
    }

    private fun buildLocationKeyForTimeZone(saved: com.example.fajrapp.data.SavedLocation?): String {
        return if (saved == null) {
            "${currentCoordinates.latitude}|${currentCoordinates.longitude}|${_uiState.value.locationName}"
        } else {
            "${saved.latitude}|${saved.longitude}|${saved.cityName}"
        }
    }

    private fun resolveCountryCode(cityLabel: String?): String? {
        if (cityLabel.isNullOrBlank()) return null
        val countryPart = cityLabel.substringAfterLast(',', "").trim()
        if (countryPart.isBlank()) return null

        if (countryPart.length == 2 && countryPart.all { it.isLetter() }) {
            return countryPart.uppercase(Locale.US)
        }

        val normalizedCountry = normalizeToken(countryPart)
        if (normalizedCountry.isEmpty()) return null

        val localesToTry = listOf(currentAppLocale(), Locale.ENGLISH, Locale("ru"), Locale("kk"))
        for (iso in Locale.getISOCountries()) {
            for (locale in localesToTry) {
                val displayCountry = Locale("", iso).getDisplayCountry(locale)
                if (normalizeToken(displayCountry) == normalizedCountry) {
                    return iso
                }
            }
        }
        return null
    }

    private fun getTimeZoneCandidates(countryCode: String?): List<String> {
        if (countryCode.isNullOrBlank()) return emptyList()
        return try {
            IcuTimeZone.getAvailableIDs(countryCode).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun selectBestTimeZoneId(
        candidateIds: List<String>,
        cityLabel: String,
        longitude: Double
    ): String? {
        if (candidateIds.isEmpty()) return null
        if (candidateIds.size == 1) return candidateIds.first()

        val normalizedCity = normalizeToken(cityLabel.substringBefore(',').trim())
        if (normalizedCity.isNotEmpty()) {
            candidateIds.firstOrNull { id ->
                val zoneCity = id.substringAfter('/').substringAfterLast('/').replace('_', ' ')
                val normalizedZoneCity = normalizeToken(zoneCity)
                normalizedZoneCity.contains(normalizedCity) || normalizedCity.contains(normalizedZoneCity)
            }?.let { return it }
        }

        return candidateIds.minByOrNull { id ->
            val zone = TimeZone.getTimeZone(id)
            val meridian = (zone.rawOffset / 3_600_000.0) * 15.0
            longitudeDistanceDegrees(longitude, meridian)
        }
    }

    private fun longitudeDistanceDegrees(a: Double, b: Double): Double {
        val diff = abs(a - b)
        return minOf(diff, 360.0 - diff)
    }

    private fun normalizeToken(value: String): String {
        return value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]"), "")
    }

    private fun formatCoordinate(value: Double): String {
        return String.format(Locale.US, "%.6f", value)
    }

    private fun localizeLocationName(rawName: String): String {
        return LocationNameLocalizer.localizeForUi(rawName, currentAppLocale())
    }

    private fun appLanguageCode(): String {
        val raw = prefsManager.getLanguage().orEmpty()
        val normalized = when (raw.lowercase(Locale.US)) {
            "kz" -> "kk"
            "id" -> "in"
            else -> raw.lowercase(Locale.US)
        }
        return normalized.ifBlank { Locale.getDefault().language.lowercase(Locale.US) }
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

}
