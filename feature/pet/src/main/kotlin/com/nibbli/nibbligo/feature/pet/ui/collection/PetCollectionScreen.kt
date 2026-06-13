package com.nibbli.nibbligo.feature.pet.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.feature.pet.presentation.PetCollectionViewModel

@Composable
fun PetCollectionScreen(
    modifier: Modifier = Modifier,
    viewModel: PetCollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "LCD collection",
            subtitle = "Wearables, scenes, and floor props for your P1 shell.",
        )
        NibbliCard(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = "${uiState.unlockedCount} of ${uiState.entries.size} unlocked",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Earn items through care, daily quests, arcade wins, and evolution.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.entries.forEach { entry ->
            LcdCollectionRow(entry = entry)
        }
    }
}

@Composable
private fun LcdCollectionRow(entry: LcdCollectionEntry) {
    NibbliCard(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (entry.unlocked) "Unlocked" else "Locked",
                style = MaterialTheme.typography.labelMedium,
                color = if (entry.unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (!entry.unlocked) {
            Text(
                text = "Unlock: ${entry.unlockHint}",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
