package com.example.fajrapp.localization

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fajrapp.MainActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocaleDirectionStartupSmokeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun resetLanguageToEnglish() {
        saveLanguage("en")
    }

    @Test
    fun startupLayoutDirectionMatchesLocale() {
        verifyLayoutDirection("ar", View.LAYOUT_DIRECTION_RTL)
        verifyLayoutDirection("ru", View.LAYOUT_DIRECTION_LTR)
        verifyLayoutDirection("en", View.LAYOUT_DIRECTION_LTR)
    }

    private fun verifyLayoutDirection(languageCode: String, expectedDirection: Int) {
        saveLanguage(languageCode)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ActivityScenario.launch<MainActivity>(launchIntent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(expectedDirection, activity.window.decorView.layoutDirection)
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

