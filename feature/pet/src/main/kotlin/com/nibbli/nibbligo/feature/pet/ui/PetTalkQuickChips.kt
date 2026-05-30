package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PetTalkQuickChips(
    isGeneratingDialogue: Boolean,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isGeneratingDialogue) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        PetTalkActionChip(
            label = "Stop",
            onClick = onStopClick,
            enabled = true,
            selected = true,
        )
    }
}
