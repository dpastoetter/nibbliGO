package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliActionTile
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.feature.settings.presentation.CompanionViewModel

@Composable
fun CompanionScreen(
    modifier: Modifier = Modifier,
    onOpenCollection: (() -> Unit)? = null,
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
            petSoundHapticsEnabled = uiState.petSoundHapticsEnabled,
            onPersonalityChange = viewModel::setPetPersonality,
            onCommentOnAgentWorkChange = viewModel::setPetCommentOnAgentWork,
            onMoodPulseModeChange = viewModel::setPetMoodPulseMode,
            onPetSoundHapticsChange = viewModel::setPetSoundHapticsEnabled,
        )
        onOpenCollection?.let { openCollection ->
            NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
                Text("LCD collection", style = MaterialTheme.typography.titleMedium)
                Text(
                    "See locked and unlocked wearables, scenes, and floor props.",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NibbliActionTile(
                    icon = Icons.Outlined.Collections,
                    label = "View collection",
                    onClick = openCollection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}
