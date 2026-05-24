package com.example.fajrapp.data

import android.content.Context
import android.content.SharedPreferences

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
        private const val KEY_OFFSET_PREFIX = "offset_"
    }

    fun saveLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANG, languageCode).apply()
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

    fun savePrayerOffset(prayerKey: String, minutes: Int) {
        prefs.edit().putInt(KEY_OFFSET_PREFIX + prayerKey, minutes).apply()
    }

    fun getPrayerOffset(prayerKey: String): Int {
        return prefs.getInt(KEY_OFFSET_PREFIX + prayerKey, 0)
    }

    fun getPrayerOffsets(prayerKeys: List<String>): Map<String, Int> {
        return prayerKeys.associateWith { getPrayerOffset(it) }
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
