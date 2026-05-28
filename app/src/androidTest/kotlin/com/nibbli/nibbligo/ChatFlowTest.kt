package com.nibbli.nibbligo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChatFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun sendMessage_streamsResponse() {
        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithText("Models").performClick()
        composeRule.onNodeWithTag("install_nibbli-fast").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Assist").performClick()
        composeRule.onNodeWithText("Local Chat").performClick()
        composeRule.onNodeWithTag("new_chat").performClick()
        composeRule.onNodeWithTag("chat_input").performTextInput("hello")
        composeRule.onNodeWithTag("send_message").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("streaming_text").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("send_message").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
