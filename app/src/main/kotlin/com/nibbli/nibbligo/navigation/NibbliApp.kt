package com.nibbli.nibbligo.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nibbli.nibbligo.feature.actions.ui.ActionsScreen
import com.nibbli.nibbligo.feature.audio.ui.AudioScribeScreen
import com.nibbli.nibbligo.feature.benchmark.ui.BenchmarkScreen
import com.nibbli.nibbligo.feature.agent.ui.AgentChatScreen
import com.nibbli.nibbligo.feature.chat.ui.ChatScreen
import com.nibbli.nibbligo.feature.image.ui.AskImageScreen
import com.nibbli.nibbligo.feature.models.ui.ModelsScreen
import com.nibbli.nibbligo.feature.pet.ui.PetHomeScreen
import com.nibbli.nibbligo.feature.promptlab.ui.PromptLabScreen
import com.nibbli.nibbligo.feature.settings.ui.SettingsScreen
import com.nibbli.nibbligo.ui.AssistHubScreen
import com.nibbli.nibbligo.ui.ManageHubScreen
import com.nibbli.nibbligo.ui.SenseHubScreen

@Composable
fun NibbliApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val visibleDestinations = TopLevelDestination.entries.filter { destination ->
        when (destination) {
            TopLevelDestination.Sense,
            TopLevelDestination.Do,
            -> false
            else -> true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                visibleDestinations.forEach { destination ->
                    val selected = currentRoute?.startsWith(destination.route) == true ||
                        when (destination) {
                            TopLevelDestination.Assist -> currentRoute in listOf(
                                Routes.CHAT,
                                Routes.AGENT_CHAT,
                                Routes.PROMPT_LAB,
                            )
                            TopLevelDestination.Sense -> currentRoute in listOf(
                                Routes.ASK_IMAGE,
                                Routes.AUDIO_SCRIBE,
                            )
                            TopLevelDestination.Do -> currentRoute == Routes.ACTIONS
                            TopLevelDestination.Manage -> currentRoute in listOf(
                                Routes.MODELS,
                                Routes.SETTINGS,
                            )
                            else -> false
                        }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PET,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.PET) {
                PetHomeScreen(
                    onNavigateToAssist = {
                        navController.navigate(Routes.AGENT_CHAT) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.ASSIST) { AssistHubScreen(navController) }
            composable(Routes.CHAT) { ChatScreen() }
            composable(Routes.AGENT_CHAT) { AgentChatScreen() }
            composable(Routes.PROMPT_LAB) { PromptLabScreen() }
            composable(Routes.SENSE) { SenseHubScreen(navController) }
            composable(Routes.ASK_IMAGE) { AskImageScreen() }
            composable(Routes.AUDIO_SCRIBE) { AudioScribeScreen() }
            composable(Routes.DO) { ActionsScreen() }
            composable(Routes.MODELS) { ModelsScreen() }
            composable(Routes.BENCHMARK) { BenchmarkScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.MANAGE) { ManageHubScreen(navController) }
        }
    }
}
