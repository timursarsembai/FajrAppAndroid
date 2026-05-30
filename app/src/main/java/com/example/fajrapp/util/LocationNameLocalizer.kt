package com.example.fajrapp.util

import android.icu.text.Transliterator
import java.util.Locale

object LocationNameLocalizer {

    private enum class ScriptGroup { LATIN, CYRILLIC, ARABIC, DEVANAGARI }

    private val cyrillicUiLanguages = setOf("ru", "kk", "ky", "tg", "tt")
    private val arabicUiLanguages = setOf("ar", "fa", "ur")
    private val devanagariUiLanguages = setOf("hi")

    private val cyrillicRegex = Regex("\\p{IsCyrillic}")
    private val latinRegex = Regex("\\p{IsLatin}")
    private val arabicRegex = Regex("\\p{IsArabic}")
    private val devanagariRegex = Regex("\\p{IsDevanagari}")
    private val lettersRegex = Regex("\\p{L}")

    fun localizeForUi(rawName: String, locale: Locale = Locale.getDefault()): String {
        if (rawName.isBlank()) return rawName

        val input = rawName.trim()
        val language = locale.language.lowercase(Locale.US)
        val targetScript = targetScriptForLanguage(language)

        if (!lettersRegex.containsMatchIn(input)) return input
        if (isMostlyTargetScript(input, targetScript)) return input

        val latinPivot = transliterate(input, "Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")
            ?.let(::normalizeLatinPivot)
            ?.takeIf { it.isNotBlank() }
            ?: input

        val directCandidate = transliterateToTarget(input, targetScript)
        if (isAcceptableForTarget(directCandidate, targetScript)) {
            return directCandidate!!
        }

        val pivotCandidate = transliterateToTarget(latinPivot, targetScript)
        if (isAcceptableForTarget(pivotCandidate, targetScript)) {
            return pivotCandidate!!
        }

        // Fallback: at least keep it readable in Latin without source-script leakage.
        return latinPivot
    }

    private fun targetScriptForLanguage(language: String): ScriptGroup {
        return when {
            language in arabicUiLanguages -> ScriptGroup.ARABIC
            language in devanagariUiLanguages -> ScriptGroup.DEVANAGARI
            language in cyrillicUiLanguages -> ScriptGroup.CYRILLIC
            else -> ScriptGroup.LATIN
        }
    }

    private fun transliterateToTarget(input: String, targetScript: ScriptGroup): String? {
        val id = when (targetScript) {
            ScriptGroup.LATIN -> "Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC"
            ScriptGroup.CYRILLIC -> "Any-Cyrillic"
            ScriptGroup.ARABIC -> "Any-Arab"
            ScriptGroup.DEVANAGARI -> "Any-Devanagari"
        }
        return transliterate(input, id)?.trim()
    }

    private fun isMostlyTargetScript(text: String, targetScript: ScriptGroup): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty()) return true

        val targetCount = letters.count { char ->
            when (targetScript) {
                ScriptGroup.LATIN -> latinRegex.matches(char.toString())
                ScriptGroup.CYRILLIC -> cyrillicRegex.matches(char.toString())
                ScriptGroup.ARABIC -> arabicRegex.matches(char.toString())
                ScriptGroup.DEVANAGARI -> devanagariRegex.matches(char.toString())
            }
        }
        return targetCount.toDouble() / letters.length.toDouble() >= 0.9
    }

    private fun isAcceptableForTarget(value: String?, targetScript: ScriptGroup): Boolean {
        if (value.isNullOrBlank()) return false
        if (!lettersRegex.containsMatchIn(value)) return false
        return isMostlyTargetScript(value, targetScript)
    }

    private fun normalizeLatinPivot(text: String): String {
        return text
            .replace('\u02BC', '\'')
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun transliterate(input: String, id: String): String? {
        return runCatching { Transliterator.getInstance(id).transliterate(input) }.getOrNull()
    }
}
