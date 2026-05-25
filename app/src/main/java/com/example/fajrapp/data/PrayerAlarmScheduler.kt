package com.example.fajrapp.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.TimeZone as IcuTimeZone
import android.os.Build
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.fajrapp.model.PrayerAlarm
import com.example.fajrapp.model.RepeatMode
import com.example.fajrapp.viewmodel.SettingsViewModel
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class PrayerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefsManager = PreferencesManager(context)

    fun scheduleAllEnabled() {
        prefsManager.getPrayerAlarms()
            .filter { it.enabled }
            .forEach { schedule(it) }
    }

    fun schedule(alarm: PrayerAlarm, nowMillis: Long = System.currentTimeMillis()) {
        cancel(alarm.id)
        if (!alarm.enabled) return

        val triggerAtMillis = computeNextTriggerAtMillis(alarm, nowMillis) ?: return
        val pendingIntent = buildPendingIntent(context, alarm.id, alarm.prayerKey, alarm.offsetMinutes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            return
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    fun cancel(alarmId: Int) {
        alarmManager.cancel(buildPendingIntent(context, alarmId, "", 0))
    }

    private fun computeNextTriggerAtMillis(alarm: PrayerAlarm, nowMillis: Long): Long? {
        val savedLocation = prefsManager.getSavedLocation()
        val coordinates = if (savedLocation != null) {
            Coordinates(savedLocation.latitude, savedLocation.longitude)
        } else {
            Coordinates(43.238949, 76.945465)
        }

        val method = SettingsViewModel.toCalculationMethod(prefsManager.getCalculationMethod())
        val madhab = SettingsViewModel.toMadhab(prefsManager.getMadhab())
        val params = method.parameters.apply { this.madhab = madhab }
        val prayerOffsets = prefsManager.getPrayerOffsets(SettingsViewModel.PRAYER_OFFSET_KEYS)

        val now = Date(nowMillis + 1_000L)
        val workingCalendar = Calendar.getInstance()

        for (dayOffset in 0..14) {
            val dateCalendar = (workingCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, dayOffset)
            }
            if (!isAlarmActiveForDay(alarm, dateCalendar)) continue

            val todayComponents = DateComponents(
                dateCalendar.get(Calendar.YEAR),
                dateCalendar.get(Calendar.MONTH) + 1,
                dateCalendar.get(Calendar.DAY_OF_MONTH)
            )

            val tomorrowCalendar = (dateCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            val tomorrowComponents = DateComponents(
                tomorrowCalendar.get(Calendar.YEAR),
                tomorrowCalendar.get(Calendar.MONTH) + 1,
                tomorrowCalendar.get(Calendar.DAY_OF_MONTH)
            )

            val todayTimes = PrayerTimes(coordinates, todayComponents, params)
            val tomorrowTimes = PrayerTimes(coordinates, tomorrowComponents, params)

            val prayerTime = resolvePrayerTimeForDay(
                prayerKey = alarm.prayerKey,
                todayTimes = todayTimes,
                tomorrowFajr = tomorrowTimes.fajr,
                prayerOffsets = prayerOffsets,
                dstShiftMinutes = getConfiguredDstShiftMinutes(
                    now = Date(todayTimes.fajr.time),
                    savedLocation = savedLocation
                )
            ) ?: continue

            val triggerAt = prayerTime.time + alarm.offsetMinutes * 60_000L
            if (triggerAt > now.time) {
                return triggerAt
            }
        }
        return null
    }

    private fun isAlarmActiveForDay(alarm: PrayerAlarm, dayCalendar: Calendar): Boolean {
        if (alarm.repeatMode != RepeatMode.CUSTOM_DAYS) return true
        val isoDay = toIsoDayOfWeek(dayCalendar.get(Calendar.DAY_OF_WEEK))
        return isoDay in alarm.repeatDaysIso
    }

    private fun toIsoDayOfWeek(calendarDayOfWeek: Int): Int {
        return if (calendarDayOfWeek == Calendar.SUNDAY) 7 else calendarDayOfWeek - 1
    }

    private fun resolvePrayerTimeForDay(
        prayerKey: String,
        todayTimes: PrayerTimes,
        tomorrowFajr: Date,
        prayerOffsets: Map<String, Int>,
        dstShiftMinutes: Int
    ): Date? {
        val base = when (prayerKey) {
            "fajr" -> applyOffset(todayTimes.fajr, prayerOffsets[SettingsViewModel.OFFSET_FAJR] ?: 0)
            "duha" -> {
                val sunriseWithOffset = applyOffset(todayTimes.sunrise, prayerOffsets[SettingsViewModel.OFFSET_SUNRISE] ?: 0)
                applyOffset(sunriseWithOffset, 20)
            }
            "dhuhr" -> applyOffset(todayTimes.dhuhr, prayerOffsets[SettingsViewModel.OFFSET_DHUHR] ?: 0)
            "asr" -> applyOffset(todayTimes.asr, prayerOffsets[SettingsViewModel.OFFSET_ASR] ?: 0)
            "maghrib" -> applyOffset(todayTimes.maghrib, prayerOffsets[SettingsViewModel.OFFSET_MAGHRIB] ?: 0)
            "isha" -> applyOffset(todayTimes.isha, prayerOffsets[SettingsViewModel.OFFSET_ISHA] ?: 0)
            "tahajjud" -> calculateTahajjudStart(todayTimes.isha, tomorrowFajr)
            else -> null
        } ?: return null

        return applyOffset(base, dstShiftMinutes)
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

    private fun getConfiguredDstShiftMinutes(now: Date, savedLocation: SavedLocation?): Int {
        return when (SettingsViewModel.normalizeDstMode(prefsManager.getDstMode())) {
            SettingsViewModel.DST_MODE_MINUS_ONE_HOUR -> -60
            SettingsViewModel.DST_MODE_PLUS_ONE_HOUR -> 60
            else -> getAutoDstShiftMinutes(now, savedLocation)
        }
    }

    private fun getAutoDstShiftMinutes(now: Date, savedLocation: SavedLocation?): Int {
        val timeZone = resolveRegionTimeZone(savedLocation) ?: return 0
        return if (timeZone.useDaylightTime() && timeZone.inDaylightTime(now)) 60 else 0
    }

    private fun resolveRegionTimeZone(savedLocation: SavedLocation?): TimeZone? {
        val countryCode = resolveCountryCode(savedLocation?.cityName) ?: return TimeZone.getDefault()
        val candidateIds = try {
            IcuTimeZone.getAvailableIDs(countryCode).toList()
        } catch (_: Exception) {
            emptyList()
        }
        if (candidateIds.isEmpty()) return TimeZone.getDefault()
        if (candidateIds.size == 1) return TimeZone.getTimeZone(candidateIds.first())

        val cityLabel = savedLocation?.cityName.orEmpty()
        val normalizedCity = normalizeToken(cityLabel.substringBefore(',').trim())
        if (normalizedCity.isNotBlank()) {
            candidateIds.firstOrNull { id ->
                val zoneCity = id.substringAfter('/').substringAfterLast('/').replace('_', ' ')
                val normalizedZoneCity = normalizeToken(zoneCity)
                normalizedZoneCity.contains(normalizedCity) || normalizedCity.contains(normalizedZoneCity)
            }?.let { return TimeZone.getTimeZone(it) }
        }

        val lon = savedLocation?.longitude ?: 0.0
        val best = candidateIds.minByOrNull { id ->
            val zone = TimeZone.getTimeZone(id)
            val meridian = (zone.rawOffset / 3_600_000.0) * 15.0
            longitudeDistanceDegrees(lon, meridian)
        }
        return if (best != null) TimeZone.getTimeZone(best) else TimeZone.getDefault()
    }

    private fun resolveCountryCode(cityLabel: String?): String? {
        if (cityLabel.isNullOrBlank()) return null
        val countryPart = cityLabel.substringAfterLast(',', "").trim()
        if (countryPart.isBlank()) return null
        if (countryPart.length == 2 && countryPart.all { it.isLetter() }) {
            return countryPart.uppercase(Locale.US)
        }

        val normalizedCountry = normalizeToken(countryPart)
        if (normalizedCountry.isBlank()) return null
        val locales = listOf(Locale.getDefault(), Locale.ENGLISH, Locale("ru"), Locale("kk"))
        for (iso in Locale.getISOCountries()) {
            for (locale in locales) {
                val name = Locale("", iso).getDisplayCountry(locale)
                if (normalizeToken(name) == normalizedCountry) {
                    return iso
                }
            }
        }
        return null
    }

    private fun normalizeToken(value: String): String {
        return value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{Nd}]"), "")
    }

    private fun longitudeDistanceDegrees(a: Double, b: Double): Double {
        val diff = abs(a - b)
        return minOf(diff, 360.0 - diff)
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.fajrapp.ACTION_PRAYER_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"

        fun buildPendingIntent(
            context: Context,
            alarmId: Int,
            prayerKey: String,
            offsetMinutes: Int
        ): PendingIntent {
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_PRAYER_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_PRAYER_KEY, prayerKey)
                putExtra(EXTRA_OFFSET_MINUTES, offsetMinutes)
            }
            return PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
