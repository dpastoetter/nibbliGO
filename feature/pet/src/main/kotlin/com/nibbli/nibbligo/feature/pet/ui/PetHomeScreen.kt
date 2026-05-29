package com.nibbli.nibbligo.feature.pet.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliAmbientBackground
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.ui.LoadingState
import com.nibbli.nibbligo.feature.pet.presentation.PetViewModel
import com.nibbli.nibbligo.feature.pet.ui.voice.rememberVoiceAssistLauncher

@Composable
fun PetHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: PetViewModel = hiltViewModel(),
    onNavigateToAssist: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCosmeticsSheet by remember { mutableStateOf(false) }

    val launchVoiceAssist = rememberVoiceAssistLauncher(
        onListening = { viewModel.setVoiceListening(true) },
        onResult = { transcript ->
            viewModel.setVoiceListening(false)
            viewModel.submitVoiceToAssist(transcript)
            onNavigateToAssist()
        },
        onError = { viewModel.onVoiceAssistError(it) },
        onStopped = { viewModel.setVoiceListening(false) },
    )

    // Home is "active" only while this screen is composed (Home tab) and the app is in the foreground.
    DisposableEffect(lifecycleOwner) {
        var foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        fun syncHomeActive() {
            viewModel.setHomeActive(foreground)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    foreground = true
                    syncHomeActive()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    foreground = false
                    syncHomeActive()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        syncHomeActive()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setHomeActive(false)
        }
    }

    LaunchedEffect(uiState.agentToast) {
        uiState.agentToast?.let {
            snackbar.showSnackbar(it)
            viewModel.clearAgentToast()
        }
    }

    if (uiState.isLoading) {
        LoadingState(modifier)
        return
    }

    val pet = uiState.petState
    val talkEnabled = pet.isAlive && !uiState.isGeneratingDialogue && !uiState.isVoiceListening

    Box(modifier = modifier.fillMaxSize()) {
        NibbliAmbientBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            PetHomeHeader(
                pet = pet,
                statusMessage = uiState.statusMessage,
                onLooksClick = { showCosmeticsSheet = true },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PetCharacterCard(
                            pet = pet,
                            onPetTap = { viewModel.onPetTapped() },
                            onCareAction = { viewModel.onInteraction(it) },
                            modifier = Modifier.fillMaxWidth(0.94f),
                        )
                    }

                    PetCompanionPanel(
                        dialogueLine = pet.dialogueLine,
                        isGeneratingDialogue = uiState.isGeneratingDialogue,
                        previousLines = uiState.recentDialogue,
                        stats = pet.stats,
                        talkEnabled = talkEnabled,
                        isVoiceListening = uiState.isVoiceListening,
                        onChipSelected = { viewModel.onTalkSend(it) },
                        onTalkToMeClick = launchVoiceAssist,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            if (pet.condition == PetCondition.DEAD) {
                PetDeadBanner(onHatch = { viewModel.hatchNewEgg() })
            }

            PetQuickActionStrip(
                cosmeticsCount = pet.unlockedCosmetics.size,
                catchEnabled = pet.isAlive,
                onCatch = { viewModel.openMinigame() },
                onDiary = {
                    context.startActivity(Intent.createChooser(viewModel.exportDiary(), "Export diary"))
                },
                onLooks = { showCosmeticsSheet = true },
            )

            SnackbarHost(hostState = snackbar, modifier = Modifier.padding(8.dp))
        }
    }

    PetCosmeticsSheet(
        visible = showCosmeticsSheet,
        pet = pet,
        onDismiss = { showCosmeticsSheet = false },
        onEquip = { viewModel.onEquipCosmetic(it) },
    )
    PetTalkSheet(
        visible = uiState.showTalkSheet,
        isGenerating = uiState.isGeneratingDialogue,
        onDismiss = { viewModel.dismissTalkSheet() },
        onSend = { viewModel.onTalkSend(it) },
    )
    PetMinigameDialog(
        visible = uiState.showMinigame,
        onDismiss = { viewModel.dismissMinigame() },
        onWin = { viewModel.onMinigameWin() },
    )
}
