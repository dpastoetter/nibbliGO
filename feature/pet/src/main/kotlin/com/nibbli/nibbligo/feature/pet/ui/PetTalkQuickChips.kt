package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PetTalkQuickChips(
    enabled: Boolean,
    isGeneratingDialogue: Boolean,
    isVoiceListening: Boolean,
    onChipSelected: (String) -> Unit,
    onStopClick: () -> Unit,
    onTalkToMeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PetTalkToMeButton(
            enabled = enabled,
            isListening = isVoiceListening,
            onClick = onTalkToMeClick,
        )
        PetTalkActionChip(
            label = "How are you?",
            onClick = { if (enabled) onChipSelected("How are you?") },
            enabled = enabled,
        )
        if (isGeneratingDialogue) {
            PetTalkActionChip(
                label = "Stop",
                onClick = onStopClick,
                enabled = true,
                selected = true,
            )
        }
    }
}
