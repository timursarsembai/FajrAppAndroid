package com.example.fajrapp.viewmodel

import android.app.Application
import android.content.Context
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import com.example.fajrapp.FajrApp
import com.example.fajrapp.R
import com.example.fajrapp.data.PreferencesManager
import com.example.fajrapp.data.PrayerAlarmScheduler
import com.example.fajrapp.model.PrayerAlarm
import com.example.fajrapp.model.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class PrayerAlarmOption(
    val key: String,
    val label: String
)

data class RingtoneOption(
    val uri: String?,
    val title: String
)

data class PrayerAlarmUiState(
    val alarms: List<PrayerAlarm> = emptyList()
)

class PrayerAlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PreferencesManager(application)
    private val scheduler = PrayerAlarmScheduler(application)
    private var cachedLocaleCode: String? = null
    private var prayerOptionsCache: List<PrayerAlarmOption> = emptyList()
    private var ringtoneOptionsCache: List<RingtoneOption> = emptyList()

    private val _uiState = MutableStateFlow(PrayerAlarmUiState())
    val uiState: StateFlow<PrayerAlarmUiState> = _uiState.asStateFlow()

    val prayerOptions: List<PrayerAlarmOption>
        get() {
            ensureLocalizedCaches()
            return prayerOptionsCache
        }

    val ringtoneOptions: List<RingtoneOption>
        get() {
            ensureLocalizedCaches()
            return ringtoneOptionsCache
        }

    private fun buildRingtoneOptions(): List<RingtoneOption> {
        val result = mutableListOf(
            RingtoneOption(
                uri = null,
                title = getString(R.string.alarm_ringtone_system_default)
            )
        )
        val appContext = getApplication<Application>().applicationContext
        val manager = RingtoneManager(appContext).apply {
            setType(RingtoneManager.TYPE_ALARM)
        }
        val cursor = manager.cursor
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty()
                    if (title.isNotBlank()) {
                        result.add(RingtoneOption(uri = uri, title = title))
                    }
                }
            } finally {
                cursor.close()
            }
        }
        return result
    }

    private fun ensureLocalizedCaches() {
        val localeCode = appLanguageCode()
        if (cachedLocaleCode == localeCode && prayerOptionsCache.isNotEmpty() && ringtoneOptionsCache.isNotEmpty()) {
            return
        }
        cachedLocaleCode = localeCode
        prayerOptionsCache = listOf(
            PrayerAlarmOption("fajr", getString(R.string.prayer_fajr)),
            PrayerAlarmOption("duha", getString(R.string.prayer_sunrise)),
            PrayerAlarmOption("dhuhr", getString(R.string.prayer_dhuhr)),
            PrayerAlarmOption("asr", getString(R.string.prayer_asr)),
            PrayerAlarmOption("maghrib", getString(R.string.prayer_maghrib)),
            PrayerAlarmOption("isha", getString(R.string.prayer_isha)),
            PrayerAlarmOption("tahajjud", getString(R.string.prayer_tahajjud))
        )
        ringtoneOptionsCache = buildRingtoneOptions()
    }

    init {
        loadAlarms()
        scheduler.scheduleAllEnabled()
    }

    fun addAlarm(
        prayerKey: String,
        offsetMinutes: Int,
        repeatMode: String,
        repeatDaysIso: Set<Int>,
        ringtoneUri: String?,
        ringtoneTitle: String
    ) {
        val boundedOffset = offsetMinutes.coerceIn(-720, 720)
        val current = _uiState.value.alarms
        val nextId = (current.maxOfOrNull { it.id } ?: 0) + 1
        val updated = current + PrayerAlarm(
            id = nextId,
            prayerKey = prayerKey,
            offsetMinutes = boundedOffset,
            enabled = true,
            repeatMode = normalizeRepeatMode(repeatMode),
            repeatDaysIso = normalizeRepeatDays(repeatMode, repeatDaysIso),
            ringtoneUri = ringtoneUri,
            ringtoneTitle = ringtoneTitle
        )
        persist(updated)
        scheduler.schedule(updated.last())
    }

    fun toggleAlarmEnabled(alarmId: Int, enabled: Boolean) {
        val updated = _uiState.value.alarms.map { alarm ->
            if (alarm.id == alarmId) alarm.copy(enabled = enabled) else alarm
        }
        persist(updated)
        updated.firstOrNull { it.id == alarmId }?.let {
            if (it.enabled) scheduler.schedule(it) else scheduler.cancel(it.id)
        }
    }

    fun deleteAlarm(alarmId: Int) {
        val updated = _uiState.value.alarms.filterNot { it.id == alarmId }
        persist(updated)
        scheduler.cancel(alarmId)
    }

    fun getAlarmById(alarmId: Int): PrayerAlarm? {
        return _uiState.value.alarms.firstOrNull { it.id == alarmId }
    }

    fun updateAlarm(
        alarmId: Int,
        prayerKey: String,
        offsetMinutes: Int,
        repeatMode: String,
        repeatDaysIso: Set<Int>,
        ringtoneUri: String?,
        ringtoneTitle: String
    ) {
        val boundedOffset = offsetMinutes.coerceIn(-720, 720)
        val target = _uiState.value.alarms.firstOrNull { it.id == alarmId } ?: return
        val updated = _uiState.value.alarms.map { alarm ->
            if (alarm.id == alarmId) {
                alarm.copy(
                    prayerKey = prayerKey,
                    offsetMinutes = boundedOffset,
                    repeatMode = normalizeRepeatMode(repeatMode),
                    repeatDaysIso = normalizeRepeatDays(repeatMode, repeatDaysIso),
                    ringtoneUri = ringtoneUri,
                    ringtoneTitle = ringtoneTitle
                )
            } else {
                alarm
            }
        }
        persist(updated)
        if (target.enabled) {
            updated.firstOrNull { it.id == alarmId }?.let { scheduler.schedule(it) }
        }
    }

    private fun loadAlarms() {
        _uiState.value = PrayerAlarmUiState(alarms = sortAlarms(prefsManager.getPrayerAlarms()))
    }

    private fun persist(alarms: List<PrayerAlarm>) {
        prefsManager.savePrayerAlarms(alarms)
        _uiState.value = PrayerAlarmUiState(alarms = sortAlarms(alarms))
    }

    private fun getString(resId: Int): String {
        return getLocalizedContext().getString(resId)
    }

    private fun sortAlarms(alarms: List<PrayerAlarm>): List<PrayerAlarm> {
        val prayerOrder = mapOf(
            "fajr" to 0,
            "duha" to 1,
            "dhuhr" to 2,
            "asr" to 3,
            "maghrib" to 4,
            "isha" to 5,
            "tahajjud" to 6
        )
        return alarms.sortedWith(
            compareBy<PrayerAlarm>(
                { prayerOrder[it.prayerKey] ?: Int.MAX_VALUE },
                { it.offsetMinutes },
                { it.id }
            )
        )
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

    private fun getLocalizedContext(): Context {
        return FajrApp.updateBaseContextLocale(
            getApplication<Application>(),
            appLanguageCode()
        )
    }

    private fun normalizeRepeatMode(mode: String): String {
        return when (mode) {
            RepeatMode.CUSTOM_DAYS -> RepeatMode.CUSTOM_DAYS
            else -> RepeatMode.WEEKLY
        }
    }

    private fun normalizeRepeatDays(mode: String, days: Set<Int>): Set<Int> {
        return if (normalizeRepeatMode(mode) == RepeatMode.CUSTOM_DAYS) {
            days.filter { it in 1..7 }.toSet().ifEmpty { setOf(1) }
        } else {
            RepeatMode.ALL_WEEK_DAYS
        }
    }
}
