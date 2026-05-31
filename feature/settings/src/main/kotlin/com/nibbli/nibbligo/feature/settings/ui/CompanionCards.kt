package com.nibbli.nibbligo.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliSecondaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.designsystem.component.NibbliTextField
import com.nibbli.nibbligo.core.model.ModelCatalog
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetPersonality

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CompanionProfileCard(
    petName: String,
    caretakerName: String,
    aboutYou: String,
    companionGoal: String,
    saveMessage: String?,
    onPetNameChange: (String) -> Unit,
    onCaretakerNameChange: (String) -> Unit,
    onAboutYouChange: (String) -> Unit,
    onCompanionGoalChange: (String) -> Unit,
    onSave: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vibeSuggestions = listOf(
        "Cheer me up when I'm having a rough day",
        "Keep me company on my home screen",
        "Playful back-and-forth",
        "Gentle, low-key buddy",
    )
    NibbliCard(modifier = modifier.padding(top = 12.dp)) {
        Text("Your companion", style = MaterialTheme.typography.titleMedium)
        Text(
            "Update names and personal touch — used in on-device talk prompts.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NibbliTextField(
            value = petName,
            onValueChange = onPetNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Pet name") },
            singleLine = true,
        )
        NibbliTextField(
            value = caretakerName,
            onValueChange = onCaretakerNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("Your name") },
            singleLine = true,
        )
        Text(
            "How they want you to show up",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("About you") },
            placeholder = { Text("Night owl, coffee fan, loves hiking…") },
            minLines = 2,
        )
        NibbliPrimaryButton(
            text = "Save companion profile",
            onClick = {
                onClearMessage()
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        saveMessage?.let { msg ->
            Text(
                msg,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun PetMemoryCard(
    petName: String,
    memoryDraft: String,
    saveMessage: String?,
    onMemoryChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NibbliCard(modifier = modifier.padding(top = 12.dp)) {
        Text("What $petName remembers", style = MaterialTheme.typography.titleMedium)
        Text(
            "Short facts nibbli keeps for talk context. Stored on your phone only.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NibbliTextField(
            value = memoryDraft,
            onValueChange = onMemoryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            placeholder = { Text("Likes snacks • Said hi after arcade win…") },
            minLines = 3,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NibbliPrimaryButton(
                text = "Save memory",
                onClick = {
                    onClearMessage()
                    onSave()
                },
                modifier = Modifier.weight(1f),
            )
            NibbliSecondaryButton(
                text = "Clear",
                onClick = {
                    onClearMessage()
                    onClear()
                },
                modifier = Modifier.weight(1f),
            )
        }
        saveMessage?.let { msg ->
            Text(
                msg,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CompanionTalkModelCard(
    selectedModelId: String?,
    installedModelIds: List<String>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    NibbliCard(modifier = modifier.padding(top = 12.dp)) {
        Text("Talk model", style = MaterialTheme.typography.titleMedium)
        Text(
            "Which on-device model powers Home talk, chips, and mood lines.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ModelPreferencePicker(
            label = "Pixel Friend model",
            hint = "Auto picks a small chat model if installed. Download more under Manage → Models.",
            selectedModelId = selectedModelId,
            installedModelIds = installedModelIds,
            onSelect = onSelect,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CompanionBehaviorCard(
    petPersonality: PetPersonality,
    petCommentOnAgentWork: Boolean,
    petMoodPulseMode: PetMoodPulseMode,
    onPersonalityChange: (PetPersonality) -> Unit,
    onCommentOnAgentWorkChange: (Boolean) -> Unit,
    onMoodPulseModeChange: (PetMoodPulseMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    NibbliCard(modifier = modifier.padding(top = 12.dp)) {
        Text("Behavior", style = MaterialTheme.typography.titleMedium)
        Text(
            "Personality and ambient behavior on your home screen.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Personality", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            PetPersonality.entries.forEach { p ->
                NibbliSuggestionChip(
                    label = p.name.lowercase().replaceFirstChar { it.titlecase() },
                    selected = petPersonality == p,
                    onClick = { onPersonalityChange(p) },
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Comment on Chat",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = petCommentOnAgentWork,
                onCheckedChange = onCommentOnAgentWorkChange,
            )
        }
        Text(
            "Mood pulse",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            PetMoodPulseMode.entries.forEach { mode ->
                NibbliSuggestionChip(
                    label = when (mode) {
                        PetMoodPulseMode.OFF -> "off"
                        PetMoodPulseMode.NORMAL -> "normal"
                        PetMoodPulseMode.QUIET -> "quiet"
                    },
                    selected = petMoodPulseMode == mode,
                    onClick = { onMoodPulseModeChange(mode) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModelPreferencePicker(
    label: String,
    hint: String,
    selectedModelId: String?,
    installedModelIds: List<String>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            hint,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (installedModelIds.isEmpty()) {
            Text(
                "No models installed yet.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            return@Column
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            NibbliSuggestionChip(
                label = "Auto",
                selected = selectedModelId == null,
                onClick = { onSelect(null) },
            )
            installedModelIds.forEach { modelId ->
                NibbliSuggestionChip(
                    label = ModelCatalog.displayName(modelId),
                    selected = selectedModelId == modelId,
                    onClick = { onSelect(modelId) },
                )
            }
        }
    }
}
