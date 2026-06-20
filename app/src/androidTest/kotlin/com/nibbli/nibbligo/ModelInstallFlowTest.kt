package com.nibbli.nibbligo

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ModelInstallFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
        Assume.assumeTrue(
            "Requires network for model download",
            isNetworkAvailable(),
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = composeRule.activity.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @Test
    fun installModel_showsInstalledBadge() {
        composeRule.onNodeWithText("Manage").performClick()
        composeRule.onNodeWithText("Models").performClick()
        composeRule.onNodeWithTag("install_functiongemma-270m").performClick()
        composeRule.waitUntil(timeoutMillis = 600_000) {
            composeRule.onAllNodesWithTag("installed_functiongemma-270m").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
