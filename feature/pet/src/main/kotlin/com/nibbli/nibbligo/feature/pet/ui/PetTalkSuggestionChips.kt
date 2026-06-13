package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip

private val DEFAULT_CHIPS = listOf(
    "How are you?",
    "Good morning",
    "Let's play!",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetTalkSuggestionChips(
    enabled: Boolean,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        DEFAULT_CHIPS.forEach { chip ->
            NibbliSuggestionChip(
                label = chip,
                selected = false,
                enabled = enabled,
                onClick = { onChipClick(chip) },
            )
        }
    }
}
