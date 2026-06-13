package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetStats

@Composable
fun PetStatStrip(
    stats: PetStats,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        ThemeStatMeter(
            label = "HUN",
            value = stats.hunger,
            warnBelow = 40,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        ThemeStatMeter(
            label = "MOOD",
            value = stats.mood,
            warnBelow = 40,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        ThemeStatMeter(
            label = "NRG",
            value = stats.energy,
            warnBelow = 40,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
        ThemeStatMeter(
            label = "TRU",
            value = stats.trust,
            warnBelow = 30,
            compact = compact,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThemeStatMeter(
    label: String,
    value: Int,
    warnBelow: Int,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val clamped = value.coerceIn(0, 100)
    val low = clamped < warnBelow
    val fillColor = if (low) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val barHeight = if (compact) 6.dp else 8.dp

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        LinearProgressIndicator(
            progress = { clamped / 100f },
            color = fillColor,
            trackColor = trackColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (compact) 2.dp else 4.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(4.dp)),
        )
        if (!compact) {
            Text(
                text = clamped.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (low) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
    }
}
