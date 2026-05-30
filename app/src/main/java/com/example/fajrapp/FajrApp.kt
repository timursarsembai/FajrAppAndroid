package com.example.fajrapp

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

class FajrApp : Application() {

    override fun attachBaseContext(base: Context) {
        // Apply resolved app locale before context is attached.
        val langCode = ensureAppLanguagePreference(base)
        super.attachBaseContext(updateBaseContextLocale(base, langCode))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val langCode = ensureAppLanguagePreference(this)
        updateBaseContextLocale(this, langCode)
    }

    companion object {
        private const val PREFS_NAME = "fajr_prefs"
        private const val KEY_LANG = "app_language"
        private const val DEFAULT_LANGUAGE = "en"
        private val SUPPORTED_LANGUAGE_CODES = setOf(
            "en", "ru", "kk", "es", "ar", "in", "ms", "ur", "hi", "uz", "ky", "tt", "fa", "tg", "fr"
        )

        private fun normalizeLanguageCode(code: String): String {
            return when (code.lowercase(Locale.US)) {
                "kz" -> "kk"
                "id" -> "in"
                else -> code.lowercase(Locale.US)
            }
        }

        fun ensureAppLanguagePreference(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_LANG, null).orEmpty().trim()
            val resolved = resolveLanguageForApp(stored, resolveDeviceLanguageCode(context))

            if (stored.isBlank() || normalizeLanguageCode(stored) != resolved) {
                prefs.edit().putString(KEY_LANG, resolved).apply()
            }
            return resolved
        }

        private fun resolveDeviceLanguageCode(context: Context): String {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
            val language = locale?.language.orEmpty()
            return normalizeLanguageCode(language)
        }

        fun resolveLanguageForApp(
            storedLanguageCode: String?,
            deviceLanguageCode: String?
        ): String {
            val stored = storedLanguageCode.orEmpty().trim()
            if (stored.isNotBlank()) {
                return normalizeLanguageCode(stored).takeIf { it in SUPPORTED_LANGUAGE_CODES } ?: DEFAULT_LANGUAGE
            }

            val device = normalizeLanguageCode(deviceLanguageCode.orEmpty().trim())
            return device.takeIf { it in SUPPORTED_LANGUAGE_CODES } ?: DEFAULT_LANGUAGE
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
