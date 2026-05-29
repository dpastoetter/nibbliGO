package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Style
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliActionTile

@Composable
fun PetQuickActionStrip(
    cosmeticsCount: Int,
    catchEnabled: Boolean,
    onCatch: () -> Unit,
    onDiary: () -> Unit,
    onLooks: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NibbliActionTile(
            icon = Icons.Outlined.SportsEsports,
            label = "Catch",
            enabled = catchEnabled,
            onClick = onCatch,
            modifier = Modifier.weight(1f),
        )
        NibbliActionTile(
            icon = Icons.Outlined.AutoStories,
            label = "Diary",
            onClick = onDiary,
            modifier = Modifier.weight(1f),
        )
        NibbliActionTile(
            icon = Icons.Outlined.Style,
            label = if (cosmeticsCount > 0) "Looks" else "None yet",
            enabled = cosmeticsCount > 0,
            onClick = onLooks,
            modifier = Modifier.weight(1f),
        )
    }
}
