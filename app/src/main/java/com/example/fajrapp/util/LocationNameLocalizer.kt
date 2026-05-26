package com.example.fajrapp.util

import android.icu.text.Transliterator
import java.util.Locale

object LocationNameLocalizer {

    private val cyrillicUiLanguages = setOf("ru", "kk", "ky", "tg", "tt")
    private val cyrillicRegex = Regex("\\p{IsCyrillic}")

    fun localizeForUi(rawName: String, locale: Locale = Locale.getDefault()): String {
        if (rawName.isBlank()) return rawName

        val language = locale.language.lowercase(Locale.US)
        if (!cyrillicRegex.containsMatchIn(rawName)) return rawName
        if (language in cyrillicUiLanguages) return rawName

        // For Arabic UI try script transliteration first; fallback to Latin transliteration.
        if (language == "ar") {
            transliterate(rawName, "Any-Arab")?.let { arabic ->
                if (arabic.isNotBlank() && arabic != rawName) return arabic
            }
        }

        return transliterate(rawName, "Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC") ?: rawName
    }

    private fun transliterate(input: String, id: String): String? {
        return runCatching { Transliterator.getInstance(id).transliterate(input) }.getOrNull()
    }
}

