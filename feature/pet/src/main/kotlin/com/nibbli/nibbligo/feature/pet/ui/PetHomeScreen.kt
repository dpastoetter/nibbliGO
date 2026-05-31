package com.nibbli.nibbligo.feature.pet.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nibbli.nibbligo.feature.pet.ui.visit.PetVisitSheet
import com.nibbli.nibbligo.feature.pet.ui.voice.rememberVoiceAssistLauncher

@Composable
fun PetHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: PetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val launchVoiceTalk = rememberVoiceAssistLauncher(
        onListening = { viewModel.setVoiceListening(true) },
        onResult = { transcript ->
            viewModel.setVoiceListening(false)
            viewModel.onTalkSend(transcript)
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
    val visitPostcard = uiState.visitPostcard
    val displayPet = visitPostcard?.toVisitDisplayState() ?: pet
    val displayDialogue = visitPostcard?.dialogueLine ?: pet.dialogueLine
    val isUserTalkGenerating = uiState.talkLcdMode && uiState.isGeneratingDialogue
    val talkEnabled = pet.isAlive && !isUserTalkGenerating &&
        !uiState.isVoiceListening && !uiState.isWarmingModel
    val micEnabled = talkEnabled && uiState.petModelInstalled

    Box(modifier = modifier.fillMaxSize()) {
        NibbliAmbientBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            PetHomeHeader(
                pet = pet,
                petModelLabel = uiState.petModelLabel,
                statusMessage = uiState.statusMessage,
                isWarmingModel = uiState.isWarmingModel,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (pet.condition == PetCondition.DEAD) {
                    PetDeadBanner(
                        onHatch = { viewModel.hatchNewEgg() },
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
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
                            pet = displayPet,
                            carePet = pet,
                            visitLabel = visitPostcard?.senderName,
                            onPetTap = { viewModel.onPetTapped() },
                            onCareAction = { viewModel.onInteraction(it) },
                            onEquipLcdItem = viewModel::onEquipLcdItem,
                            onLcdActivity = viewModel::onLcdActivity,
                            dialogueLine = if (uiState.talkLcdMode) pet.dialogueLine else displayDialogue,
                            isGeneratingDialogue = uiState.isGeneratingDialogue,
                            talkLcdMode = uiState.talkLcdMode,
                            onDismissTalkLcd = viewModel::dismissTalkLcdMode,
                            modifier = Modifier.fillMaxWidth(0.94f),
                        )
                    }

                    PetCompanionPanel(
                        stats = pet.stats,
                        isGeneratingDialogue = uiState.isGeneratingDialogue,
                        talkHistory = uiState.talkHistory,
                        streamingDialogue = pet.dialogueLine,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                PetTalkInputBar(
                    enabled = talkEnabled,
                    micEnabled = micEnabled,
                    isPetAlive = pet.isAlive,
                    isUserTalkGenerating = isUserTalkGenerating,
                    isVoiceListening = uiState.isVoiceListening,
                    onTalkToMeClick = launchVoiceTalk,
                    isWarmingModel = uiState.isWarmingModel,
                    onSend = { viewModel.onTalkSend(it) },
                    onStopClick = { viewModel.stopGeneration() },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            PetQuickActionStrip(
                playEnabled = pet.isAlive,
                shareEnabled = pet.isAlive,
                onPlay = { viewModel.openMinigame() },
                onShare = {
                    context.startActivity(
                        Intent.createChooser(viewModel.shareTodayCard(), "Share nibbli"),
                    )
                },
                onPostcard = { viewModel.openPostcardSheet() },
                onDiary = {
                    context.startActivity(Intent.createChooser(viewModel.exportDiary(), "Export diary"))
                },
            )

            SnackbarHost(hostState = snackbar, modifier = Modifier.padding(8.dp))
        }
    }

    PetTalkSheet(
        visible = uiState.showTalkSheet,
        isGenerating = uiState.isGeneratingDialogue,
        onDismiss = { viewModel.dismissTalkSheet() },
        onSend = { viewModel.onTalkSend(it) },
    )
    PetMinigameDialog(
        visible = uiState.showMinigame,
        dailyTargetScore = pet.engagement.dailyCatchTargetScore,
        ghostChallengeScore = uiState.catchGhostScore,
        onDismiss = {
            viewModel.clearCatchGhost()
            viewModel.dismissMinigame()
        },
        onWin = { viewModel.onMinigameWin() },
        onGameEnd = viewModel::onMinigameEnd,
    )
    uiState.evolutionShareStage?.let { stage ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissEvolutionSharePrompt() },
            title = { Text("${pet.name} evolved!") },
            text = { Text("Show your friends your new ${stage.name.lowercase()}!") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.shareEvolutionCard()?.let { intent ->
                        context.startActivity(Intent.createChooser(intent, "Share evolution"))
                    }
                    viewModel.dismissEvolutionSharePrompt()
                }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissEvolutionSharePrompt() }) {
                    Text("Later")
                }
            },
        )
    }
    PetVisitSheet(
        visible = uiState.showPostcardSheet,
        pet = pet,
        visitPostcard = uiState.visitPostcard,
        onScanResult = viewModel::importVisitFromQr,
        onShareQr = {
            context.startActivity(Intent.createChooser(viewModel.shareVisitQr(), "Share visit QR"))
        },
        onDismissVisit = viewModel::endPostcardVisit,
        onDismiss = viewModel::dismissPostcardSheet,
    )
}
