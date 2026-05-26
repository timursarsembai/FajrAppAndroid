package com.example.fajrapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fajrapp.MainActivity
import com.example.fajrapp.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun openSettingsAndLanguageScreen() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed().performClick()

        val settingsTitle = composeRule.activity.getString(R.string.settings_title)
        val languageTitle = composeRule.activity.getString(R.string.settings_language)

        composeRule.onNodeWithText(settingsTitle).assertIsDisplayed()
        composeRule.onNodeWithText(languageTitle).assertIsDisplayed().performClick()
        composeRule.onNodeWithText(languageTitle).assertIsDisplayed()
    }
}
