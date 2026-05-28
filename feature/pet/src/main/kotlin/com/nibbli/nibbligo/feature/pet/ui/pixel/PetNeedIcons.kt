package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetNeed

@Composable
fun PetNeedIcons(need: PetNeed, modifier: Modifier = Modifier) {
    if (need == PetNeed.NONE) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = iconFor(need),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = labelFor(need),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun iconFor(need: PetNeed): String = when (need) {
    PetNeed.HUNGRY -> "🍗"
    PetNeed.DIRTY -> "🛁"
    PetNeed.TIRED -> "💤"
    PetNeed.SICK -> "💊"
    PetNeed.UNHAPPY -> "💧"
    PetNeed.LONELY -> "📢"
    else -> "❗"
}

private fun labelFor(need: PetNeed): String = when (need) {
    PetNeed.HUNGRY -> "Hungry"
    PetNeed.DIRTY -> "Messy"
    PetNeed.TIRED -> "Tired"
    PetNeed.SICK -> "Sick"
    PetNeed.UNHAPPY -> "Sad"
    PetNeed.LONELY -> "Lonely"
    else -> ""
}
