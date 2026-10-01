package com.kanwarai.voiceprofessionalmessage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeRendersAndSelectionsChange() {
        composeRule.onNodeWithText("Voice to Message").assertIsDisplayed()
        composeRule.onNodeWithText("Message").assertIsSelected()
        composeRule.onNodeWithText("Professional").assertIsSelected()

        composeRule.onNodeWithText("Email").performClick().assertIsSelected()
        composeRule.onNodeWithText("Friendly").performClick().assertIsSelected()

        composeRule.onNodeWithContentDescription(
            "Record voice note. Available in Phase 3.",
        ).performClick()
        composeRule.onNodeWithText("Voice recording will be added in Phase 3.")
            .assertIsDisplayed()
    }

    @Test
    fun allShellRoutesOpenAndReturnHome() {
        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithText("No messages yet.").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("Open message area").performClick()
        composeRule.onNodeWithText("No generated message yet.").assertIsDisplayed()
        pressBack()

        composeRule.onNodeWithText("Voice to Message").assertIsDisplayed()
    }

    @Test
    fun navigationAndHomeSelectionsSurviveActivityRecreation() {
        composeRule.onNodeWithText("Email").performClick()
        composeRule.onNodeWithText("Concise").performClick()
        composeRule.onNodeWithText("History").performClick()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("No messages yet.").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("Email").assertIsSelected()
        composeRule.onNodeWithText("Concise").assertIsSelected()
    }

    @Test
    fun themeSelectionSurvivesActivityRecreation() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("System").assertIsSelected()

        composeRule.onNodeWithText("Dark").performClick().assertIsSelected()
        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Dark").assertIsSelected()
        composeRule.onNodeWithText("Privacy").assertIsDisplayed()
    }
}
