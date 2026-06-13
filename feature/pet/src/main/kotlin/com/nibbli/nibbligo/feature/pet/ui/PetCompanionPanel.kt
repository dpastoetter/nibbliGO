package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.feature.pet.presentation.TalkHistoryEntry

@Composable
fun PetCompanionPanel(
    stats: PetStats,
    isGeneratingDialogue: Boolean,
    talkHistory: List<TalkHistoryEntry>,
    streamingDialogue: String,
    talkLcdMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dimmed = talkLcdMode
    NibbliCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .alpha(if (dimmed) 0.45f else 1f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (dimmed) {
                Text(
                    text = "Mood ${stats.mood} · Trust ${stats.trust} · Energy ${stats.energy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                PetStatStrip(stats = stats)
                PetTalkHistory(
                    talkHistory = talkHistory,
                    streamingDialogue = streamingDialogue,
                    isGeneratingDialogue = isGeneratingDialogue,
                )
            }
        }
    }
}
