package com.nibbli.nibbligo.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nibbli.nibbligo.core.designsystem.component.NibbliBottomNavItem
import com.nibbli.nibbligo.core.designsystem.component.NibbliBottomNavigationBar
import com.nibbli.nibbligo.core.designsystem.component.isKeyboardVisible
import com.nibbli.nibbligo.feature.actions.ui.ActionsScreen
import com.nibbli.nibbligo.feature.audio.ui.AudioScribeScreen
import com.nibbli.nibbligo.feature.agent.ui.AgentChatScreen
import com.nibbli.nibbligo.feature.benchmark.ui.BenchmarkScreen
import com.nibbli.nibbligo.feature.chat.ui.ChatScreen
import com.nibbli.nibbligo.feature.promptlab.ui.PromptLabScreen
import com.nibbli.nibbligo.feature.image.ui.AskImageScreen
import com.nibbli.nibbligo.feature.models.ui.ModelsScreen
import com.nibbli.nibbligo.feature.pet.ui.PetHomeScreen
import com.nibbli.nibbligo.feature.pet.ui.collection.PetCollectionScreen
import com.nibbli.nibbligo.feature.settings.ui.BasicAiKnowledgeScreen
import com.nibbli.nibbligo.feature.settings.ui.CompanionScreen
import com.nibbli.nibbligo.feature.settings.ui.NibbliFaqScreen
import com.nibbli.nibbligo.feature.settings.ui.ParentControlsScreen
import com.nibbli.nibbligo.feature.settings.ui.SettingsScreen
import com.nibbli.nibbligo.presentation.MainViewModel
import com.nibbli.nibbligo.ui.ManageHubScreen
import com.nibbli.nibbligo.ui.SenseHubScreen

@Composable
fun NibbliApp(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val agentNavSignal by viewModel.agentNavigationSignal.collectAsStateWithLifecycle(initialValue = 0)

    LaunchedEffect(agentNavSignal) {
        if (agentNavSignal > 0) {
            navController.navigate(Routes.AGENT) {
                launchSingleTop = true
            }
        }
    }

    val visibleDestinations = TopLevelDestination.entries.filter { destination ->
        when (destination) {
            TopLevelDestination.Sense,
            TopLevelDestination.Do,
            -> false
            else -> true
        }
    }

    val keyboardVisible = isKeyboardVisible()

    Scaffold(
        bottomBar = {
            if (!keyboardVisible) {
                NibbliBottomNavigationBar(
                    items = visibleDestinations.map { destination ->
                    val selected = currentRoute?.startsWith(destination.route) == true ||
                        when (destination) {
                            TopLevelDestination.Sense -> currentRoute in listOf(
                                Routes.ASK_IMAGE,
                                Routes.AUDIO_SCRIBE,
                            )
                            TopLevelDestination.Do -> currentRoute == Routes.ACTIONS
                            TopLevelDestination.Manage -> currentRoute in listOf(
                                Routes.MODELS,
                                Routes.SETTINGS,
                                Routes.COMPANION,
                                Routes.BENCHMARK,
                                Routes.PROMPT_LAB,
                                Routes.AGENT,
                                Routes.FAQ_NIBBLIGO,
                                Routes.FAQ_AI_BASICS,
                                Routes.PET_COLLECTION,
                                Routes.PARENT_CONTROLS,
                            )
                            else -> false
                        }
                    NibbliBottomNavItem(
                        label = destination.label,
                        icon = destination.icon,
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
                    )
                    },
                )
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
                    onNavigateToCollection = { navController.navigate(Routes.PET_COLLECTION) },
                )
            }
            composable(Routes.CHAT) { ChatScreen(navController = navController) }
            composable(Routes.AGENT) { AgentChatScreen() }
            composable(Routes.SENSE) { SenseHubScreen(navController) }
            composable(Routes.ASK_IMAGE) { AskImageScreen() }
            composable(Routes.AUDIO_SCRIBE) { AudioScribeScreen() }
            composable(Routes.DO) { ActionsScreen() }
            composable(Routes.MODELS) { ModelsScreen() }
            composable(Routes.BENCHMARK) { BenchmarkScreen() }
            composable(Routes.PROMPT_LAB) { PromptLabScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.COMPANION) {
                CompanionScreen(onOpenCollection = { navController.navigate(Routes.PET_COLLECTION) })
            }
            composable(Routes.PET_COLLECTION) { PetCollectionScreen() }
            composable(Routes.FAQ_NIBBLIGO) { NibbliFaqScreen() }
            composable(Routes.FAQ_AI_BASICS) { BasicAiKnowledgeScreen() }
            composable(Routes.PARENT_CONTROLS) { ParentControlsScreen() }
            composable(Routes.MANAGE) { ManageHubScreen(navController) }
        }
    }
}
