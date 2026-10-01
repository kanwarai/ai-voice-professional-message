package com.kanwarai.voiceprofessionalmessage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun allShellRoutesOpenAndReturnHome() {
        composeRule.onNodeWithText("AI Voice Note").assertIsDisplayed()

        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithText("No messages yet.").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Settings are coming in later phases.").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("View result screen").performClick()
        composeRule.onNodeWithText("No generated message yet.").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("Record voice note").performClick()
        composeRule.onNodeWithText("Voice recording will be added in Phase 3.")
            .assertIsDisplayed()
    }

    @Test
    fun currentRouteSurvivesActivityRecreation() {
        composeRule.onNodeWithText("History").performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("No messages yet.").assertIsDisplayed()
    }
}
