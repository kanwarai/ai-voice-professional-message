package com.kanwarai.voiceprofessionalmessage

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test

class NavigationSmokeTest {
    private val permissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(permissionRule).around(composeRule)

    @Test
    fun homeRendersAndSelectionsChange() {
        composeRule.onNodeWithText("Voice to Message").assertIsDisplayed()
        composeRule.onNodeWithText("Message").assertIsSelected()
        composeRule.onNodeWithText("Professional").assertIsSelected()

        composeRule.onNodeWithText("Email").performClick().assertIsSelected()
        composeRule.onNodeWithText("Friendly").performClick().assertIsSelected()

    }

    @Test
    fun grantedPermissionShowsRealRecordingAndCancelStates() {
        composeRule.onNodeWithContentDescription("Record voice note").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Recording").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Recording").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription("Record voice note")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Record voice note").assertIsDisplayed()
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
