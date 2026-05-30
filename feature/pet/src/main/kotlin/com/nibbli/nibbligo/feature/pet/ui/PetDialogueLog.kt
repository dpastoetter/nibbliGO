package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.feature.pet.presentation.TalkHistoryEntry
import com.nibbli.nibbligo.feature.pet.presentation.TalkHistoryRole

@Composable
fun PetTalkHistory(
    talkHistory: List<TalkHistoryEntry>,
    streamingDialogue: String,
    isGeneratingDialogue: Boolean,
    talkLcdMode: Boolean,
    modifier: Modifier = Modifier,
) {
    if (talkHistory.isEmpty() && !talkLcdMode) return

    val scrollState = rememberScrollState()
    LaunchedEffect(talkHistory.size, streamingDialogue) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .verticalScroll(scrollState),
    ) {
        talkHistory.forEach { entry ->
            val isLastPetEntry = entry.role == TalkHistoryRole.PET &&
                entry == talkHistory.lastOrNull { it.role == TalkHistoryRole.PET }
            val text = when (entry.role) {
                TalkHistoryRole.USER -> "You: ${entry.text}"
                TalkHistoryRole.PET -> {
                    if (isLastPetEntry && isGeneratingDialogue && streamingDialogue.isNotBlank()) {
                        "nibbli: $streamingDialogue"
                    } else {
                        "nibbli: ${entry.text}"
                    }
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (entry.role == TalkHistoryRole.USER) {
                    FontWeight.Normal
                } else {
                    FontWeight.Medium
                },
                color = when (entry.role) {
                    TalkHistoryRole.USER -> MaterialTheme.colorScheme.onSurfaceVariant
                    TalkHistoryRole.PET -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isGeneratingDialogue && streamingDialogue.isBlank() &&
            talkHistory.lastOrNull()?.role == TalkHistoryRole.USER
        ) {
            Text(
                text = "nibbli: …",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
