package com.example.fajrapp.localization

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fajrapp.MainActivity
import com.example.fajrapp.R
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeLanguageCycleSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun cycleLanguages_updatesHomePrayerLabelWithoutManualRestart() {
        // Keep Arabic in the middle to verify RTL -> LTR and reverse transitions at runtime.
        val languageCodes = listOf(
            "en", "ru", "kk", "ky", "es", "ar", "in", "ms", "ur", "hi", "uz", "tt", "fa", "tg", "fr"
        )

        languageCodes.forEach { languageCode ->
            openLanguageSelectionFromHome()
            composeRule.onNodeWithTag("LanguageList")
                .performScrollToNode(hasContentDescription("lang_$languageCode"))
            composeRule.onNodeWithContentDescription("lang_$languageCode").performClick()

            returnToHomeFromLanguageSelection()

            val expectedFajr = expectedLocalizedString(languageCode, R.string.prayer_fajr)
            composeRule.waitUntil(12_000) {
                composeRule.onAllNodesWithText(expectedFajr).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onAllNodesWithText(expectedFajr)[0].assertIsDisplayed()
        }
    }

    private fun openLanguageSelectionFromHome() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Open Language Selection")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Open Language Selection").performClick()
    }

    private fun returnToHomeFromLanguageSelection() {
        // languages -> settings
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        // settings -> home
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun expectedLocalizedString(languageCode: String, resId: Int): String {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val normalized = normalizeLanguageCode(languageCode)
        val locale = Locale(normalized)
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(locale)
        val localizedContext = appContext.createConfigurationContext(config)
        return localizedContext.resources.getString(resId)
    }

    private fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase(Locale.US)) {
            "kz" -> "kk"
            "id" -> "in"
            else -> code.lowercase(Locale.US)
        }
    }
}
