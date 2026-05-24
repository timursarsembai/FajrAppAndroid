package com.example.fajrapp.viewmodel

import android.app.Application
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.fajrapp.R
import com.example.fajrapp.data.LocationManager
import com.example.fajrapp.data.PreferencesManager
import com.example.fajrapp.data.SavedLocation
import com.example.fajrapp.model.PrayerData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

data class PrayerUiState(
    val prayerTimes: List<PrayerData> = emptyList(),
    val currentTimeFormatted: String = "00:00:00",
    val locationName: String = "",
    val hijriDate: String = "",
    val gregorianDate: String = "",
    val isLoading: Boolean = true
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    private val locationManager = LocationManager(application)
    private val prefsManager = PreferencesManager(application)
    private var prayerTimesData: PrayerTimes? = null
    private var nextPrayerTime: Date? = null

    // Default coordinates (Almaty, Kazakhstan)
    private var currentCoordinates = Coordinates(43.238949, 76.945465)

    init {
        _uiState.value = _uiState.value.copy(locationName = getString(R.string.str_loading))
        loadInitialData()
    }
    
    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    private fun loadInitialData() {
        // 1. Load cached location immediately
        val saved = prefsManager.getSavedLocation()
        if (saved != null) {
            currentCoordinates = Coordinates(saved.latitude, saved.longitude)
            _uiState.value = _uiState.value.copy(
                locationName = saved.cityName,
                isLoading = false
            )
            calculatePrayerTimes()
        }

        startTimer() // Always start timer

        // 2. Fetch fresh location in background
        viewModelScope.launch {
            try {
                val location = locationManager.getCurrentLocation()
                if (location != null) {
                    val isDifferent = if (saved != null) {
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(
                            saved.latitude, saved.longitude,
                            location.latitude, location.longitude,
                            results
                        )
                        results[0] > 1000 // Update if moved > 1km
                    } else true

                    if (isDifferent) {
                        currentCoordinates = Coordinates(location.latitude, location.longitude)
                        updateLocationNameAndSave(location.latitude, location.longitude)
                        calculatePrayerTimes()
                    }
                } else if (saved == null) {
                    // Only fallback if nothing saved AND fetch failed
                    _uiState.value = _uiState.value.copy(locationName = "Almaty, Kazakhstan")
                    calculatePrayerTimes() // Default coords
                }
            } catch (e: Exception) {
                if (saved == null) {
                    _uiState.value = _uiState.value.copy(locationName = "Almaty, Kazakhstan")
                }
            }
        }
    }

    private fun updateLocationNameAndSave(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                // Use default locale for Geocoder to get localized city names if possible
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                
                val locationName = if (!addresses.isNullOrEmpty()) {
                    val city = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    val country = addresses[0].countryName ?: ""
                    if (city.isNotEmpty()) "$city, $country" else country
                } else {
                    getString(R.string.settings_location_unknown)
                }

                _uiState.value = _uiState.value.copy(locationName = locationName)
                
                // Save to cache
                prefsManager.saveLocation(lat, lon, locationName)
                
            } catch (e: Exception) {
                // Ignore errors
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

        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        params.madhab = Madhab.HANAFI

        prayerTimesData = PrayerTimes(currentCoordinates, dateComponents, params)

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Date()

        val prayersList = mutableListOf<PrayerData>()
        
        prayerTimesData?.let { times ->
            val prayerDefinitions = listOf(
                Triple(getString(R.string.prayer_fajr), times.fajr, "الفجر"),
                Triple(getString(R.string.prayer_sunrise), times.sunrise, "الشروق"),
                Triple(getString(R.string.prayer_dhuhr), times.dhuhr, "الظهر"),
                Triple(getString(R.string.prayer_asr), times.asr, "العصر"),
                Triple(getString(R.string.prayer_maghrib), times.maghrib, "المغرب"),
                Triple(getString(R.string.prayer_isha), times.isha, "العشاء")
            )

            var foundNext = false
            for ((name, time, arabicName) in prayerDefinitions) {
                val isPassed = now.after(time)
                val isNext = !isPassed && !foundNext
                if (isNext) {
                    foundNext = true
                    nextPrayerTime = time
                }

                prayersList.add(
                    PrayerData(
                        name = name,
                        arabicName = arabicName,
                        time = timeFormatter.format(time),
                        isNext = isNext,
                        isPassed = isPassed && !isNext, 
                        timeLeft = if (isNext) "${getString(R.string.timer_prefix)} --:--:--" else null
                    )
                )
            }

            if (!foundNext && prayersList.isNotEmpty()) {
                val tomorrow = Calendar.getInstance()
                tomorrow.add(Calendar.DAY_OF_MONTH, 1)
                val tomorrowComponents = DateComponents(
                    tomorrow.get(Calendar.YEAR),
                    tomorrow.get(Calendar.MONTH) + 1,
                    tomorrow.get(Calendar.DAY_OF_MONTH)
                )
                val tomorrowTimes = PrayerTimes(currentCoordinates, tomorrowComponents, params)
                nextPrayerTime = tomorrowTimes.fajr
                
                prayersList[0] = prayersList[0].copy(
                    isNext = true, 
                    isPassed = false,
                    timeLeft = "${getString(R.string.timer_prefix)} --:--:--"
                )
            }
        }

        val gregorianFormatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
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
            "${hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)} ${getHijriMonthName(hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR))} ${hijrahDate.get(java.time.temporal.ChronoField.YEAR)}"
        } else {
            "15 ${getString(R.string.hijri_rajab)} 1448" // Fallback
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
            while (true) {
                val now = Date()
                val clockFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val currentTime = clockFormatter.format(now)
                
                updateCountdown(now)
                
                _uiState.value = _uiState.value.copy(
                    currentTimeFormatted = currentTime
                )
                delay(1000L)
            }
        }
    }

    private fun updateCountdown(now: Date) {
        nextPrayerTime?.let { nextTime ->
            var diffMillis = nextTime.time - now.time

            if (diffMillis < 0) {
                calculatePrayerTimes()
                return
            }

            val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
            diffMillis -= TimeUnit.HOURS.toMillis(hours)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
            diffMillis -= TimeUnit.MINUTES.toMillis(minutes)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis)

            val formattedCountdown = String.format(Locale.getDefault(), "${getString(R.string.timer_prefix)} %02d:%02d:%02d", hours, minutes, seconds)
            
            val updatedPrayers = _uiState.value.prayerTimes.map { 
                if (it.isNext) it.copy(timeLeft = formattedCountdown) else it
            }
            
            _uiState.value = _uiState.value.copy(prayerTimes = updatedPrayers)
        }
    }
}
