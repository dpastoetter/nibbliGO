package com.nibbli.nibbligo.feature.pet.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.nibbli.nibbligo.core.designsystem.component.NibbliTextField
import com.nibbli.nibbligo.core.designsystem.component.isKeyboardVisible
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetNeedRules
import com.nibbli.nibbligo.feature.pet.presentation.PetViewModel
import com.nibbli.nibbligo.core.pet.llm.PetTalkSuggestions
import com.nibbli.nibbligo.feature.pet.ui.visit.PetVisitSheet
import com.nibbli.nibbligo.feature.pet.ui.feedback.PetFeedbackKind
import com.nibbli.nibbligo.feature.pet.ui.feedback.rememberPetFeedbackController
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
    val feedback = rememberPetFeedbackController()

    LaunchedEffect(uiState.currentCelebration, uiState.petSoundHapticsEnabled) {
        val celebration = uiState.currentCelebration ?: return@LaunchedEffect
        viewModel.feedbackKindForCelebration(celebration)?.let { kind ->
            feedback.perform(kind, uiState.petSoundHapticsEnabled)
        }
    }

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
        PetHomeLoadingState(modifier = modifier, petName = uiState.petState.name)
        return
    }

    val pet = uiState.petState
    val visitPostcard = uiState.visitPostcard
    val visitPet = visitPostcard?.toVisitDisplayState()
    val displayDialogue = visitPostcard?.dialogueLine ?: pet.dialogueLine
    val isUserTalkGenerating = uiState.talkLcdMode && uiState.isGeneratingDialogue
    val talkEnabled = pet.isAlive && !isUserTalkGenerating &&
        !uiState.isVoiceListening && !uiState.isWarmingModel
    val micEnabled = talkEnabled && uiState.petModelInstalled
    val keyboardVisible = isKeyboardVisible()
    val activeNeed = PetNeedRules.deriveNeed(pet).takeIf { it != PetNeed.NONE }
        ?: pet.activeNeed.takeIf { it != PetNeed.NONE }

    Box(modifier = modifier.fillMaxSize()) {
        NibbliAmbientBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            PetHomeHeader(
                pet = pet,
                petModelLabel = uiState.petModelLabel,
                statusMessage = uiState.statusMessage,
                isWarmingModel = uiState.isWarmingModel,
                petModelInstalled = uiState.petModelInstalled,
                isGeneratingDialogue = uiState.isGeneratingDialogue,
                onRefreshModel = viewModel::refreshPetModel,
                onQuestHint = viewModel::onQuestHint,
            )

            if (uiState.showStreakWelcomeBack && pet.isAlive) {
                PetWelcomeBackBanner(
                    streakDays = pet.engagement.careStreakDays,
                    onFeed = {
                        viewModel.dismissStreakWelcomeBack()
                        viewModel.onInteraction(PetInteraction.FEED_MEAL)
                    },
                    onPlay = {
                        viewModel.dismissStreakWelcomeBack()
                        viewModel.openMinigame()
                    },
                    onTalk = {
                        viewModel.dismissStreakWelcomeBack()
                        viewModel.onQuickChip("How are you?")
                    },
                    onDismiss = viewModel::dismissStreakWelcomeBack,
                )
            }

            if (!uiState.petModelInstalled) {
                PetModelSetupBanner(
                    visible = true,
                    isDownloading = uiState.isPetModelDownloading,
                    downloadProgress = uiState.petModelDownloadProgress,
                    isResumingDownload = uiState.petModelDownloadResuming,
                    onDownload = viewModel::downloadRecommendedPetModel,
                    startCollapsed = uiState.modelSetupPromptDismissed,
                    modelId = uiState.recommendedPetModelId,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
            ) {
                if (pet.condition == PetCondition.DEAD) {
                    PetDeadBanner(
                        onHatch = { viewModel.hatchNewEgg() },
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PetCharacterCard(
                        pet = pet,
                        visitPet = visitPet,
                        carePet = pet,
                        visitLabel = visitPostcard?.senderName,
                        onPetTap = {
                            if (uiState.petSoundHapticsEnabled) {
                                feedback.perform(PetFeedbackKind.TAP, soundEnabled = true)
                            }
                            viewModel.onPetTapped()
                        },
                        onCareAction = { viewModel.onInteraction(it) },
                        onCareConfirm = {
                            if (uiState.petSoundHapticsEnabled) {
                                feedback.perform(PetFeedbackKind.CARE_CONFIRM, soundEnabled = true)
                            }
                        },
                        onEquipLcdItem = viewModel::onEquipLcdItem,
                        onLcdActivity = viewModel::onLcdActivity,
                        dialogueLine = if (uiState.talkLcdMode) pet.dialogueLine else displayDialogue,
                        isGeneratingDialogue = uiState.isGeneratingDialogue,
                        talkLcdMode = uiState.talkLcdMode,
                        onDismissTalkLcd = viewModel::dismissTalkLcdMode,
                        showCoachMarks = uiState.showCoachMarks,
                        onDismissCoachMarks = viewModel::dismissCoachMarks,
                        openItemsModeRequest = uiState.openLcdItemsMode,
                        onItemsModeOpened = viewModel::clearOpenLcdItemsMode,
                        highlightConfirm = activeNeed != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PetCompanionPanel(
                    stats = pet.stats,
                    isGeneratingDialogue = uiState.isGeneratingDialogue,
                    talkHistory = uiState.talkHistory,
                    streamingDialogue = pet.dialogueLine,
                    talkLcdMode = uiState.talkLcdMode,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
            ) {
                if (!keyboardVisible && !isUserTalkGenerating && talkEnabled && uiState.talkHistory.isEmpty()) {
                    PetTalkSuggestionChips(
                        chips = PetTalkSuggestions.starterChips,
                        enabled = micEnabled,
                        onChipClick = viewModel::onQuickChip,
                        modifier = Modifier.padding(bottom = 4.dp),
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
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(
                            top = 8.dp,
                            bottom = if (keyboardVisible) 4.dp else 8.dp,
                        ),
                )

                if (!keyboardVisible) {
                    PetQuickActionStrip(
                        playEnabled = pet.isAlive,
                        shareEnabled = pet.isAlive,
                        onPlay = { viewModel.openMinigame() },
                        onShare = {
                            viewModel.shareTodayCard(context) { intent ->
                                context.startActivity(
                                    Intent.createChooser(intent, "Share nibbli"),
                                )
                            }
                        },
                        onPostcard = { viewModel.openPostcardSheet() },
                        onDiary = {
                            context.startActivity(Intent.createChooser(viewModel.exportDiary(), "Export diary"))
                        },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
        )
    }

    uiState.pendingMemoryProposal?.let { proposal ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissMemoryProposal() },
            title = { Text("Remember this about you?") },
            text = {
                Column {
                    Text("${pet.name} can remember this for future chats.")
                    NibbliTextField(
                        value = proposal,
                        onValueChange = viewModel::updateMemoryProposal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.approveMemoryProposal() }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMemoryProposal() }) {
                    Text("Not now")
                }
            },
        )
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
        onWin = {
            if (uiState.petSoundHapticsEnabled) {
                feedback.perform(PetFeedbackKind.MINIGAME_WIN, soundEnabled = true)
            }
            viewModel.onMinigameWin()
        },
        onGameEnd = viewModel::onMinigameEnd,
    )
    uiState.evolutionShareStage?.let { stage ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissEvolutionSharePrompt() },
            title = { Text("${pet.name} evolved!") },
            text = { Text("Show your friends your new ${stage.name.lowercase()}!") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.shareEvolutionCard(context) { intent ->
                        intent?.let {
                            context.startActivity(Intent.createChooser(it, "Share evolution"))
                        }
                        viewModel.dismissEvolutionSharePrompt()
                    }
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
    PetCelebrationSheet(
        event = uiState.currentCelebration,
        petName = pet.name,
        onDismiss = viewModel::dismissCelebration,
        onEquipNow = viewModel::equipFromCelebration,
    )
    PetModelSetupSheet(
        visible = uiState.showModelSetupSheet,
        isDownloading = uiState.isPetModelDownloading,
        downloadProgress = uiState.petModelDownloadProgress,
        isResumingDownload = uiState.petModelDownloadResuming,
        message = uiState.petModelSetupMessage,
        modelId = uiState.recommendedPetModelId,
        onDownload = viewModel::downloadRecommendedPetModel,
        onDismiss = viewModel::dismissModelSetupSheet,
        onClearMessage = viewModel::clearPetModelSetupMessage,
    )
}
