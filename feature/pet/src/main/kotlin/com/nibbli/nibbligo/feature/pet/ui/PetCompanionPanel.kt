package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.PetBubble
import com.nibbli.nibbligo.core.model.PetStats

@Composable
fun PetCompanionPanel(
    dialogueLine: String,
    isGeneratingDialogue: Boolean,
    previousLines: List<String>,
    stats: PetStats,
    talkEnabled: Boolean,
    isVoiceListening: Boolean,
    onChipSelected: (String) -> Unit,
    onStopClick: () -> Unit,
    onTalkToMeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NibbliCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PetDialogueLog(previousLines = previousLines)

            if (previousLines.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                )
            }

            PetBubble(
                text = dialogueLine,
                isLoading = isGeneratingDialogue && dialogueLine.isBlank(),
                applyHorizontalInset = false,
            )

            PetTalkQuickChips(
                enabled = talkEnabled,
                isGeneratingDialogue = isGeneratingDialogue,
                isVoiceListening = isVoiceListening,
                onChipSelected = onChipSelected,
                onStopClick = onStopClick,
                onTalkToMeClick = onTalkToMeClick,
            )

            PetStatStrip(stats = stats)
        }
    }
}
