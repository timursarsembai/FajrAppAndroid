package com.example.fajrapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fajrapp.MainActivity
import com.example.fajrapp.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeLanguageAndNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun switchLanguageRtlAndBackToLtrWithoutRestart() {
        openLanguageSelectionFromHome()
        composeRule.onNodeWithTag("LanguageList")
            .performScrollToNode(hasContentDescription("lang_ar"))
        composeRule.onNodeWithContentDescription("lang_ar").performClick()
        returnToHomeFromLanguageSelection()

        openLanguageSelectionFromHome()
        composeRule.onNodeWithTag("LanguageList")
            .performScrollToNode(hasContentDescription("lang_en"))
        composeRule.onNodeWithContentDescription("lang_en").performClick()
        returnToHomeFromLanguageSelection()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithContentDescription("Open Language Selection").assertIsDisplayed()
    }

    @Test
    fun openCalendarAndAlarmsScreens() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        val calendarTitle = composeRule.activity.getString(R.string.calendar_title)
        val alarmsTitle = composeRule.activity.getString(R.string.alarm_title)

        composeRule.onNodeWithContentDescription("Open Calendar").performClick()
        composeRule.onNodeWithText(calendarTitle).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(calendarTitle).performClick()

        composeRule.onNodeWithContentDescription("Open Alarms").performClick()
        composeRule.onNodeWithText(alarmsTitle).assertIsDisplayed()
    }

    private fun openLanguageSelectionFromHome() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithContentDescription("Open Language Selection").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Language Selection").performClick()
    }

    private fun returnToHomeFromLanguageSelection() {
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
