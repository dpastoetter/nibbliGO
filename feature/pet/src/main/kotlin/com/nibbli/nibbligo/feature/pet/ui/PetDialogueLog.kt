package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PetDialogueLog(
    previousLines: List<String>,
    modifier: Modifier = Modifier,
) {
    if (previousLines.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        previousLines.forEachIndexed { index, line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = 0.45f + (index * 0.1f).coerceAtMost(0.25f),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}
