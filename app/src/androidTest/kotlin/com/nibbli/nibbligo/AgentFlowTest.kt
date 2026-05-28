package com.nibbli.nibbligo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AgentFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun agentChat_proposesReminder_confirmCompletes() {
        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithText("Models").performClick()
        composeRule.onNodeWithTag("install_nibbli-fast").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Assist").performClick()
        composeRule.onNodeWithText("Agent Chat").performClick()
        composeRule.onNodeWithTag("agent_input").performTextInput("remind me to stretch")
        composeRule.onNodeWithTag("agent_send").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText("Confirm").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Confirm").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag("agent_message").fetchSemanticsNodes().size >= 2
        }
    }
}
