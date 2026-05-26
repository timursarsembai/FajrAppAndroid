package com.example.fajrapp.localization

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fajrapp.MainActivity
import com.example.fajrapp.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocaleStartupSmokeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun resetLanguageToEnglish() {
        saveLanguage("en")
    }

    @Test
    fun appStartsWithCyrillicAndArabicLocales() {
        verifyLocaleStartup(
            languageCode = "ru",
            expectedFajr = "Фаджр"
        )
        verifyLocaleStartup(
            languageCode = "ky",
            expectedFajr = "Фажр"
        )
        verifyLocaleStartup(
            languageCode = "kk",
            expectedFajr = "Фаджр"
        )
        verifyLocaleStartup(
            languageCode = "ar",
            expectedFajr = "الفجر"
        )
    }

    private fun verifyLocaleStartup(
        languageCode: String,
        expectedFajr: String
    ) {
        saveLanguage(languageCode)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ActivityScenario.launch<MainActivity>(launchIntent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(expectedFajr, activity.getString(R.string.prayer_fajr))
            }
        }
    }

    private fun saveLanguage(languageCode: String) {
        context.getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", languageCode)
            .commit()
    }
}
