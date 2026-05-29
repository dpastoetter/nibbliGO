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
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetStats

@Composable
fun PetStatStrip(
    stats: PetStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeStatMeter(
            label = "HUN",
            value = stats.hunger,
            warnBelow = 40,
            modifier = Modifier.weight(1f),
        )
        ThemeStatMeter(
            label = "MOOD",
            value = stats.mood,
            warnBelow = 40,
            modifier = Modifier.weight(1f),
        )
        ThemeStatMeter(
            label = "NRG",
            value = stats.energy,
            warnBelow = 40,
            modifier = Modifier.weight(1f),
        )
        ThemeStatMeter(
            label = "TRU",
            value = stats.trust,
            warnBelow = 30,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThemeStatMeter(
    label: String,
    value: Int,
    warnBelow: Int,
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

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        LinearProgressIndicator(
            progress = { clamped / 100f },
            color = fillColor,
            trackColor = trackColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Text(
            text = clamped.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (low) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}
