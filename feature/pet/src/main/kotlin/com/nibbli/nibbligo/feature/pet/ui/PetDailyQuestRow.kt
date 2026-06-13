package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetEngagement
import com.nibbli.nibbligo.core.model.PetEngagementRules

@Composable
fun PetDailyQuestRow(
    engagement: PetEngagement,
    onQuestHint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (PetEngagementRules.dailyQuestComplete(engagement)) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Daily quest",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
        QuestStep(
            label = "Feed",
            done = engagement.dailyQuestFeed,
            hint = "Open care menu → cycle to Feed → tap ●",
            onHint = onQuestHint,
        )
        QuestStep(
            label = "Play",
            done = engagement.dailyQuestPlay,
            hint = "Tap Play below or win an arcade game",
            onHint = onQuestHint,
        )
        QuestStep(
            label = "Talk",
            done = engagement.dailyQuestTalk,
            hint = "Send a message or tap Talk to me",
            onHint = onQuestHint,
        )
    }
}

@Composable
private fun QuestStep(
    label: String,
    done: Boolean,
    hint: String,
    onHint: (String) -> Unit,
) {
    val status = if (done) "$label complete" else "$label incomplete, tap for hint"
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (done) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        },
        modifier = Modifier
            .semantics { contentDescription = status }
            .clickable(enabled = !done) { onHint(hint) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (done) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
