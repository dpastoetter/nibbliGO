package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.feature.settings.presentation.CompanionViewModel

@Composable
fun CompanionScreen(
    modifier: Modifier = Modifier,
    viewModel: CompanionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "Companion",
            subtitle = "Profile, memory, talk model, and home-screen behavior.",
            showOnDeviceBadge = true,
        )
        CompanionProfileCard(
            petName = uiState.petName,
            caretakerName = uiState.caretakerName,
            aboutYou = uiState.aboutYou,
            companionGoal = uiState.companionGoal,
            saveMessage = uiState.companionSaveMessage,
            onPetNameChange = viewModel::onPetNameChange,
            onCaretakerNameChange = viewModel::onCaretakerNameChange,
            onAboutYouChange = viewModel::onAboutYouChange,
            onCompanionGoalChange = viewModel::onCompanionGoalChange,
            onSave = viewModel::saveCompanionProfile,
            onClearMessage = viewModel::clearCompanionSaveMessage,
        )
        PetMemoryCard(
            petName = uiState.petName,
            memoryDraft = uiState.memoryDraft,
            saveMessage = uiState.memorySaveMessage,
            onMemoryChange = viewModel::onMemoryDraftChange,
            onSave = viewModel::saveMemory,
            onClear = viewModel::clearMemory,
            onClearMessage = viewModel::clearMemorySaveMessage,
        )
        CompanionTalkModelCard(
            selectedModelId = uiState.petModelId,
            installedModelIds = uiState.installedModelIds,
            onSelect = viewModel::setPetModelId,
        )
        CompanionBehaviorCard(
            petPersonality = uiState.petPersonality,
            petCommentOnAgentWork = uiState.petCommentOnAgentWork,
            petMoodPulseMode = uiState.petMoodPulseMode,
            onPersonalityChange = viewModel::setPetPersonality,
            onCommentOnAgentWorkChange = viewModel::setPetCommentOnAgentWork,
            onMoodPulseModeChange = viewModel::setPetMoodPulseMode,
        )
    }
}
