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
import com.nibbli.nibbligo.core.pet.llm.PetTalkSuggestions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetTalkSuggestionChips(
    chips: List<String>,
    enabled: Boolean,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        chips.forEach { chip ->
            NibbliSuggestionChip(
                label = chip,
                selected = false,
                enabled = enabled,
                onClick = { onChipClick(chip) },
            )
        }
    }
}

@Composable
fun PetTalkSuggestionChips(
    enabled: Boolean,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PetTalkSuggestionChips(
        chips = PetTalkSuggestions.starterChips,
        enabled = enabled,
        onChipClick = onChipClick,
        modifier = modifier,
    )
}
