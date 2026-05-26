package com.example.fajrapp.data

import android.content.Context
import android.content.SharedPreferences
import com.example.fajrapp.model.PrayerAlarm
import com.example.fajrapp.model.RepeatMode
import org.json.JSONArray
import org.json.JSONObject

data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAT = "latitude"
        private const val KEY_LON = "longitude"
        private const val KEY_CITY = "city_name"
        private const val KEY_LANG = "app_language"
        private const val KEY_CALC_METHOD = "calc_method"
        private const val KEY_MADHAB = "madhab"
        private const val KEY_DST_MODE = "dst_mode"
        private const val KEY_OFFSET_PREFIX = "offset_"
        private const val KEY_PRAYER_ALARMS = "prayer_alarms"
        private const val KEY_NOTIFICATIONS_USE_SINGLE_SOUND = "notifications_use_single_sound"
        private const val KEY_NOTIFICATIONS_GLOBAL_SOUND_URI = "notifications_global_sound_uri"
        private const val KEY_NOTIFICATIONS_GLOBAL_SOUND_TITLE = "notifications_global_sound_title"
        private const val KEY_NOTIFICATIONS_PRAYER_ENABLED_PREFIX = "notifications_prayer_enabled_"
        private const val KEY_NOTIFICATIONS_PRAYER_SOUND_URI_PREFIX = "notifications_prayer_sound_uri_"
        private const val KEY_NOTIFICATIONS_PRAYER_SOUND_TITLE_PREFIX = "notifications_prayer_sound_title_"
    }

    fun saveLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANG, languageCode).commit()
    }

    fun getLanguage(): String? {
        return prefs.getString(KEY_LANG, null)
    }

    fun saveCalculationMethod(methodCode: String) {
        prefs.edit().putString(KEY_CALC_METHOD, methodCode).apply()
    }

    fun getCalculationMethod(): String? {
        return prefs.getString(KEY_CALC_METHOD, null)
    }

    fun saveMadhab(madhabCode: String) {
        prefs.edit().putString(KEY_MADHAB, madhabCode).apply()
    }

    fun getMadhab(): String? {
        return prefs.getString(KEY_MADHAB, null)
    }

    fun saveDstMode(modeCode: String) {
        prefs.edit().putString(KEY_DST_MODE, modeCode).apply()
    }

    fun getDstMode(): String? {
        return prefs.getString(KEY_DST_MODE, null)
    }

    fun savePrayerOffset(prayerKey: String, minutes: Int) {
        prefs.edit().putInt(KEY_OFFSET_PREFIX + prayerKey, minutes).apply()
    }

    fun getPrayerOffset(prayerKey: String): Int {
        return prefs.getInt(KEY_OFFSET_PREFIX + prayerKey, 0)
    }

    fun getPrayerOffsets(prayerKeys: List<String>): Map<String, Int> {
        return prayerKeys.associateWith { getPrayerOffset(it) }
    }

    fun saveNotificationsUseSingleSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_USE_SINGLE_SOUND, enabled).apply()
    }

    fun getNotificationsUseSingleSound(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_USE_SINGLE_SOUND, true)
    }

    fun saveNotificationsGlobalSound(uri: String?, title: String) {
        prefs.edit()
            .putString(KEY_NOTIFICATIONS_GLOBAL_SOUND_URI, uri ?: "")
            .putString(KEY_NOTIFICATIONS_GLOBAL_SOUND_TITLE, title)
            .apply()
    }

    fun getNotificationsGlobalSoundUri(): String? {
        return prefs.getString(KEY_NOTIFICATIONS_GLOBAL_SOUND_URI, "").orEmpty().ifBlank { null }
    }

    fun getNotificationsGlobalSoundTitle(): String {
        return prefs.getString(KEY_NOTIFICATIONS_GLOBAL_SOUND_TITLE, "").orEmpty()
    }

    fun savePrayerNotificationEnabled(prayerKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_PRAYER_ENABLED_PREFIX + prayerKey, enabled).apply()
    }

    fun getPrayerNotificationEnabled(prayerKey: String): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_PRAYER_ENABLED_PREFIX + prayerKey, true)
    }

    fun savePrayerNotificationSound(prayerKey: String, uri: String?, title: String) {
        prefs.edit()
            .putString(KEY_NOTIFICATIONS_PRAYER_SOUND_URI_PREFIX + prayerKey, uri ?: "")
            .putString(KEY_NOTIFICATIONS_PRAYER_SOUND_TITLE_PREFIX + prayerKey, title)
            .apply()
    }

    fun getPrayerNotificationSoundUri(prayerKey: String): String? {
        return prefs.getString(KEY_NOTIFICATIONS_PRAYER_SOUND_URI_PREFIX + prayerKey, "")
            .orEmpty()
            .ifBlank { null }
    }

    fun getPrayerNotificationSoundTitle(prayerKey: String): String {
        return prefs.getString(KEY_NOTIFICATIONS_PRAYER_SOUND_TITLE_PREFIX + prayerKey, "").orEmpty()
    }

    fun savePrayerAlarms(alarms: List<PrayerAlarm>) {
        val array = JSONArray()
        alarms.forEach { alarm ->
            array.put(
                JSONObject().apply {
                    put("id", alarm.id)
                    put("prayerKey", alarm.prayerKey)
                    put("offsetMinutes", alarm.offsetMinutes)
                    put("enabled", alarm.enabled)
                    put("repeatMode", alarm.repeatMode)
                    put("repeatDaysIso", alarm.repeatDaysIso.sorted().joinToString(","))
                    put("ringtoneUri", alarm.ringtoneUri ?: "")
                    put("ringtoneTitle", alarm.ringtoneTitle)
                }
            )
        }
        prefs.edit().putString(KEY_PRAYER_ALARMS, array.toString()).apply()
    }

    fun getPrayerAlarms(): List<PrayerAlarm> {
        val raw = prefs.getString(KEY_PRAYER_ALARMS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optInt("id", -1)
                    val prayerKey = item.optString("prayerKey", "")
                    if (id < 0 || prayerKey.isBlank()) continue
                    add(
                        PrayerAlarm(
                            id = id,
                            prayerKey = prayerKey,
                            offsetMinutes = item.optInt("offsetMinutes", 0),
                            enabled = item.optBoolean("enabled", true),
                            repeatMode = item.optString("repeatMode", RepeatMode.WEEKLY),
                            repeatDaysIso = parseRepeatDays(item.optString("repeatDaysIso", "")),
                            ringtoneUri = item.optString("ringtoneUri", "").ifBlank { null },
                            ringtoneTitle = item.optString("ringtoneTitle", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseRepeatDays(raw: String): Set<Int> {
        if (raw.isBlank()) return RepeatMode.ALL_WEEK_DAYS
        val parsed = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
        return if (parsed.isEmpty()) RepeatMode.ALL_WEEK_DAYS else parsed
    }

    fun saveLocation(lat: Double, lon: Double, cityName: String) {
        prefs.edit().apply {
            putString(KEY_LAT, lat.toString())
            putString(KEY_LON, lon.toString())
            putString(KEY_CITY, cityName)
            apply()
        }
    }

    fun getSavedLocation(): SavedLocation? {
        val latStr = prefs.getString(KEY_LAT, null)
        val lonStr = prefs.getString(KEY_LON, null)
        val city = prefs.getString(KEY_CITY, null)

        if (latStr != null && lonStr != null && city != null) {
            return try {
                SavedLocation(
                    latitude = latStr.toDouble(),
                    longitude = lonStr.toDouble(),
                    cityName = city
                )
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
}
