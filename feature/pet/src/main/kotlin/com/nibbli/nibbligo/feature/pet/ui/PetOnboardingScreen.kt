package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliAmbientBackground
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliSecondaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.designsystem.component.NibbliTextField
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.feature.pet.presentation.PetOnboardingViewModel

@Composable
fun PetOnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PetOnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        NibbliAmbientBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                LinearProgressIndicator(
                    progress = { (uiState.stepIndex + 1f) / uiState.stepCount },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                Text(
                    text = "Step ${uiState.stepIndex + 1} of ${uiState.stepCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )

                NibbliCard {
                    when (uiState.stepIndex) {
                        0 -> WelcomeStep()
                        1 -> PetNameStep(
                            value = uiState.petName,
                            onValueChange = viewModel::updatePetName,
                        )
                        2 -> CaretakerNameStep(
                            petName = uiState.petName.trim().ifBlank { "Pixel Friend" },
                            value = uiState.caretakerName,
                            onValueChange = viewModel::updateCaretakerName,
                        )
                        3 -> PersonalityStep(
                            selected = uiState.personality,
                            onSelect = viewModel::updatePersonality,
                        )
                        4 -> PersonalTouchStep(
                            petName = uiState.petName.trim().ifBlank { "Pixel Friend" },
                            aboutYou = uiState.aboutYou,
                            companionGoal = uiState.companionGoal,
                            onAboutYouChange = viewModel::updateAboutYou,
                            onCompanionGoalChange = viewModel::updateCompanionGoal,
                        )
                        5 -> TermsStep(
                            accepted = uiState.termsAccepted,
                            onAcceptedChange = viewModel::updateTermsAccepted,
                        )
                        6 -> ModelDownloadStep(
                            modelName = ModelCatalog.displayName(uiState.recommendedModelId),
                            sizeLabel = ModelCatalog.approximateSizeLabel(uiState.recommendedModelId),
                            isLowRamDevice = uiState.recommendedModelId == ModelCatalog.LIGHTWEIGHT_PET_MODEL_ID,
                            isDownloading = uiState.isDownloadingModel,
                            downloadProgress = uiState.downloadProgress,
                            downloadResuming = uiState.downloadResuming,
                            message = uiState.downloadMessage,
                            onDownload = viewModel::downloadRecommendedModel,
                            onClearMessage = viewModel::clearDownloadMessage,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.stepIndex > 0) {
                    NibbliSecondaryButton(
                        text = "Back",
                        onClick = viewModel::previousStep,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                val isLastStep = uiState.stepIndex >= uiState.stepCount - 1
                NibbliPrimaryButton(
                    text = when {
                        uiState.isSaving -> "Saving…"
                        isLastStep -> "Meet ${uiState.petName.trim().ifBlank { "Pixel Friend" }}"
                        else -> "Continue"
                    },
                    onClick = {
                        if (isLastStep) viewModel.complete(onFinished)
                        else viewModel.nextStep()
                    },
                    enabled = uiState.canContinue && !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(10.dp),
            ) {}
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(10.dp),
            ) {}
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(10.dp),
            ) {}
        }
        Text(
            text = "Welcome to nibbliGO",
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = "Your Pixel Friend runs entirely on your phone. " +
                "A quick setup helps your companion talk to you in a more personal way.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CaretakerNameStep(
    petName: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "What should $petName call you?",
            style = MaterialTheme.typography.titleMedium,
        )
        NibbliTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Your name or nickname") },
            singleLine = true,
        )
    }
}

@Composable
private fun PetNameStep(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Name your Pixel Friend",
            style = MaterialTheme.typography.titleMedium,
        )
        NibbliTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("nibbli") },
            singleLine = true,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalityStep(
    selected: PetPersonality,
    onSelect: (PetPersonality) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Pick a personality",
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PetPersonality.entries.forEach { personality ->
                NibbliSuggestionChip(
                    label = personality.name.lowercase().replaceFirstChar { it.titlecase() },
                    selected = selected == personality,
                    onClick = { onSelect(personality) },
                )
            }
        }
        Text(
            text = personalityBlurb(selected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalTouchStep(
    petName: String,
    aboutYou: String,
    companionGoal: String,
    onAboutYouChange: (String) -> Unit,
    onCompanionGoalChange: (String) -> Unit,
) {
    val vibeSuggestions = listOf(
        "Cheer me up when I'm having a rough day",
        "Keep me company on my home screen",
        "Playful back-and-forth",
        "Gentle, low-key buddy",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Anything $petName should remember?",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Optional — helps talk feel personal. Skip with Continue.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vibeSuggestions.forEach { suggestion ->
                NibbliSuggestionChip(
                    label = suggestion,
                    selected = companionGoal == suggestion,
                    onClick = { onCompanionGoalChange(suggestion) },
                )
            }
        }
        NibbliTextField(
            value = aboutYou,
            onValueChange = onAboutYouChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Night owl, coffee fan, loves hiking…") },
            minLines = 3,
        )
    }
}

private fun personalityBlurb(personality: PetPersonality): String = when (personality) {
    PetPersonality.PLAYFUL -> "Bouncy, cheerful, and quick with a beep."
    PetPersonality.CALM -> "Gentle, steady, and soothing on your home screen."
    PetPersonality.CURIOUS -> "Inquisitive about apps, models, and what you're up to."
}

@Composable
private fun TermsStep(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Before you meet nibbli",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Please read and accept the following to continue:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "• You agree not to use nibbliGO for anything illegal or harmful.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "• On-device generative AI can be wrong, incomplete, or inappropriate. " +
                    "Verify important information yourself.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "• The nibbliGO team is not liable for model errors, actions you take " +
                    "based on outputs, or device resource use.",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "• Pixel Friend chat runs on your phone — not sent to a cloud LLM for inference. " +
                    "You remain responsible for how you use the app.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAcceptedChange(!accepted) }
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = onAcceptedChange,
            )
            Text(
                text = "I agree to the above",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun ModelDownloadStep(
    modelName: String,
    sizeLabel: String,
    isLowRamDevice: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    downloadResuming: Boolean,
    message: String?,
    onDownload: () -> Unit,
    onClearMessage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Download nibbli's brain?",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = buildString {
                append("$modelName ($sizeLabel) runs talk, voice, and mood lines on-device.")
                if (isLowRamDevice) {
                    append(" We picked a lighter model for your device.")
                } else {
                    append(" Wi‑Fi recommended for the download.")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isDownloading) {
            LinearProgressIndicator(
                progress = { downloadProgress.coerceIn(0, 100) / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
            Text(
                text = when {
                    downloadProgress > 0 -> "Downloading… $downloadProgress%"
                    downloadResuming -> "Resuming download…"
                    else -> "Starting download…"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            NibbliPrimaryButton(
                text = "Download now",
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "Optional — tap Meet below to skip and download later from Home.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(4000)
                onClearMessage()
            }
        }
    }
}
