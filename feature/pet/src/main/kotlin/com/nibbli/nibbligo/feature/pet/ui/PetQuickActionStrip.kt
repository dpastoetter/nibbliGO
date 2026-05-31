package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliActionTile

@Composable
fun PetQuickActionStrip(
    playEnabled: Boolean,
    shareEnabled: Boolean,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDiary: () -> Unit,
    onPostcard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NibbliActionTile(
            icon = Icons.Outlined.SportsEsports,
            label = "Play",
            enabled = playEnabled,
            onClick = onPlay,
            compact = true,
            modifier = Modifier.weight(1f),
        )
        NibbliActionTile(
            icon = Icons.Outlined.Share,
            label = "Share",
            enabled = shareEnabled,
            onClick = onShare,
            compact = true,
            modifier = Modifier.weight(1f),
        )
        NibbliActionTile(
            icon = Icons.Outlined.Mail,
            label = "Visit",
            onClick = onPostcard,
            compact = true,
            modifier = Modifier.weight(1f),
        )
        NibbliActionTile(
            icon = Icons.Outlined.AutoStories,
            label = "Diary",
            onClick = onDiary,
            compact = true,
            modifier = Modifier.weight(1f),
        )
    }
}
