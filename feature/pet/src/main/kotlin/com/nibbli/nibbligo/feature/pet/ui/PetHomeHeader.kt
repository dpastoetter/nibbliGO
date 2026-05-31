package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.OnDeviceBadge
import com.nibbli.nibbligo.core.model.PetEngagementRules
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetNeedRules
import com.nibbli.nibbligo.core.model.PetState

@Composable
fun PetHomeHeader(
    pet: PetState,
    petModelLabel: String,
    statusMessage: String?,
    isWarmingModel: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val need = PetNeedRules.deriveNeed(pet).takeIf { it != PetNeed.NONE }
        ?: pet.activeNeed.takeIf { it != PetNeed.NONE }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "· ${pet.stage.name.lowercase().replaceFirstChar { it.titlecase() }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            need?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Text(
                        text = needLabel(it),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildString {
                    append("Care ${pet.careScore} · Age ${pet.ageMinutes}m")
                    if (pet.engagement.careStreakDays > 0) {
                        append(" · ${pet.engagement.careStreakDays}d streak")
                    }
                },
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OnDeviceBadge(compact = true, label = petModelLabel)
        }
        if (pet.isAlive && !PetEngagementRules.dailyQuestComplete(pet.engagement)) {
            val quest = buildList {
                if (!pet.engagement.dailyQuestFeed) add("feed")
                if (!pet.engagement.dailyQuestPlay) add("play")
                if (!pet.engagement.dailyQuestTalk) add("talk")
            }.joinToString(" · ")
            Text(
                text = "Daily quest: $quest · Play goal ${pet.engagement.dailyCatchTargetScore}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
            )
        }
        if (isWarmingModel) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Warming up model…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
        statusMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

private fun needLabel(need: PetNeed): String = when (need) {
    PetNeed.HUNGRY -> "Hungry"
    PetNeed.UNHAPPY -> "Unhappy"
    PetNeed.DIRTY -> "Dirty"
    PetNeed.TIRED -> "Tired"
    PetNeed.SICK -> "Sick"
    PetNeed.LONELY -> "Lonely"
    PetNeed.NONE -> ""
}
