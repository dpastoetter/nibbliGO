package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.feature.pet.presentation.TalkHistoryEntry

@Composable
fun PetCompanionPanel(
    stats: PetStats,
    talkEnabled: Boolean,
    isGeneratingDialogue: Boolean,
    isVoiceListening: Boolean,
    talkHistory: List<TalkHistoryEntry>,
    streamingDialogue: String,
    talkLcdMode: Boolean,
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
            PetTalkQuickChips(
                enabled = talkEnabled,
                isGeneratingDialogue = isGeneratingDialogue,
                isVoiceListening = isVoiceListening,
                onChipSelected = onChipSelected,
                onStopClick = onStopClick,
                onTalkToMeClick = onTalkToMeClick,
            )

            PetStatStrip(stats = stats)

            PetTalkHistory(
                talkHistory = talkHistory,
                streamingDialogue = streamingDialogue,
                isGeneratingDialogue = isGeneratingDialogue,
                talkLcdMode = talkLcdMode,
            )
        }
    }
}
