package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.feature.pet.presentation.TalkHistoryEntry
import com.nibbli.nibbligo.feature.pet.presentation.TalkHistoryRole

@Composable
fun PetTalkHistory(
    talkHistory: List<TalkHistoryEntry>,
    streamingDialogue: String,
    isGeneratingDialogue: Boolean,
    modifier: Modifier = Modifier,
    maxContentHeight: Dp = 220.dp,
) {
    if (talkHistory.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    val messageLabel = if (talkHistory.size == 1) "1 message" else "${talkHistory.size} messages"
    val toggleDescription = if (expanded) "Collapse talk history" else "Expand talk history"

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { contentDescription = toggleDescription }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Talk history",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (talkHistory.isNotEmpty()) {
                    Text(
                        text = messageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            PetTalkHistoryContent(
                talkHistory = talkHistory,
                streamingDialogue = streamingDialogue,
                isGeneratingDialogue = isGeneratingDialogue,
                maxContentHeight = maxContentHeight,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PetTalkHistoryContent(
    talkHistory: List<TalkHistoryEntry>,
    streamingDialogue: String,
    isGeneratingDialogue: Boolean,
    maxContentHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(talkHistory.size, streamingDialogue) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxContentHeight)
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
