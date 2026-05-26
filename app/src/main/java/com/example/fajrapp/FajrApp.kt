package com.example.fajrapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.example.fajrapp.data.PreferencesManager
import java.util.Locale

class FajrApp : Application() {

    override fun attachBaseContext(base: Context) {
        // Apply saved locale before context is attached to Activity/Application
        val prefs = base.getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_language", "en") ?: "en"
        super.attachBaseContext(updateBaseContextLocale(base, langCode))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val prefs = getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_language", "en") ?: "en"
        updateBaseContextLocale(this, langCode)
    }

    companion object {
        private fun normalizeLanguageCode(code: String): String {
            return when (code.lowercase(Locale.US)) {
                "kz" -> "kk"
                "id" -> "in"
                else -> code.lowercase(Locale.US)
            }
        }

        fun updateBaseContextLocale(context: Context, language: String): Context {
            val normalized = normalizeLanguageCode(language)
            val locale = Locale(normalized)
            Locale.setDefault(locale)
            
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            
            return context.createConfigurationContext(config)
        }
    }
}
