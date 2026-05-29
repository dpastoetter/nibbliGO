package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetTalkQuickChips(
    enabled: Boolean,
    isVoiceListening: Boolean,
    onChipSelected: (String) -> Unit,
    onTalkToMeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chips = listOf("How are you?", "Let's play!")

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        PetTalkToMeButton(
            enabled = enabled,
            isListening = isVoiceListening,
            onClick = onTalkToMeClick,
        )
        chips.forEach { phrase ->
            NibbliSuggestionChip(
                label = phrase,
                onClick = { if (enabled) onChipSelected(phrase) },
                enabled = enabled,
            )
        }
    }
}
