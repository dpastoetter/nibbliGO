package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliAmbientBackground
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.OnDeviceBadge
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OnDeviceBadge(compact = true)
                LinearProgressIndicator(
                    progress = { (uiState.stepIndex + 1f) / uiState.stepCount },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Step ${uiState.stepIndex + 1} of ${uiState.stepCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        4 -> AboutYouStep(
                            petName = uiState.petName.trim().ifBlank { "Pixel Friend" },
                            value = uiState.aboutYou,
                            onValueChange = viewModel::updateAboutYou,
                        )
                        else -> CompanionGoalStep(
                            petName = uiState.petName.trim().ifBlank { "Pixel Friend" },
                            value = uiState.companionGoal,
                            onValueChange = viewModel::updateCompanionGoal,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.stepIndex > 0) {
                    OutlinedButton(
                        onClick = viewModel::previousStep,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                val isLastStep = uiState.stepIndex >= uiState.stepCount - 1
                Button(
                    onClick = {
                        if (isLastStep) viewModel.complete(onFinished)
                        else viewModel.nextStep()
                    },
                    enabled = uiState.canContinue && !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            uiState.isSaving -> "Saving…"
                            isLastStep -> "Meet ${uiState.petName.trim().ifBlank { "Pixel Friend" }}"
                            else -> "Continue"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Welcome to nibbliGO",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "What should $petName call you?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Name your Pixel Friend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
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
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PetPersonality.entries.forEach { personality ->
                FilterChip(
                    selected = selected == personality,
                    onClick = { onSelect(personality) },
                    label = { Text(personality.name.lowercase().replaceFirstChar { it.uppercase() }) },
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

@Composable
private fun AboutYouStep(
    petName: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tell $petName a little about you",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Optional — hobbies, how you like to chat, anything that helps $petName feel like yours.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("I love retro gadgets and on-device AI…") },
            minLines = 3,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompanionGoalStep(
    petName: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    val suggestions = listOf(
        "A cozy companion on my home screen",
        "Help explaining on-device AI",
        "Someone to check in on my pet stats",
        "Light chat while I tinker with models",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "What do you want from $petName?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                FilterChip(
                    selected = value == suggestion,
                    onClick = { onValueChange(suggestion) },
                    label = { Text(suggestion) },
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Or write your own…") },
            minLines = 2,
        )
    }
}

private fun personalityBlurb(personality: PetPersonality): String = when (personality) {
    PetPersonality.PLAYFUL -> "Bouncy, cheerful, and quick with a beep."
    PetPersonality.CALM -> "Gentle, steady, and soothing on your home screen."
    PetPersonality.CURIOUS -> "Inquisitive about apps, models, and what you're up to."
}
