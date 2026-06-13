package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    petModelInstalled: Boolean = false,
    isGeneratingDialogue: Boolean = false,
    onRefreshModel: () -> Unit = {},
    onQuestHint: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val need = PetNeedRules.deriveNeed(pet).takeIf { it != PetNeed.NONE }
        ?: pet.activeNeed.takeIf { it != PetNeed.NONE }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = pet.stage.name.lowercase().replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
            need?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
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
                    append("Care ${pet.careScore}")
                    if (pet.engagement.careStreakDays > 0) {
                        append(" · ${pet.engagement.careStreakDays}d streak")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            OnDeviceBadge(compact = true, label = petModelLabel)
            if (petModelInstalled) {
                IconButton(
                    onClick = onRefreshModel,
                    enabled = !isWarmingModel && !isGeneratingDialogue,
                    modifier = Modifier.size(32.dp),
                ) {
                    if (isWarmingModel) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Reload model",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            }
        }
        if (pet.isAlive && !PetEngagementRules.dailyQuestComplete(pet.engagement)) {
            PetDailyQuestRow(
                engagement = pet.engagement,
                onQuestHint = onQuestHint,
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
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = buildString {
                        append("Warming up model…")
                        val backendHint = petModelLabel.substringAfter(" · ", "")
                        if (backendHint.isNotEmpty() && backendHint != petModelLabel) {
                            append(" ($backendHint)")
                        } else {
                            append(" (~few seconds)")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    maxLines = 2,
                )
            }
        }
        statusMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
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
