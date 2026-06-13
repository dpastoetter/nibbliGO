package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PetWelcomeBackBanner(
    streakDays: Int,
    onFeed: () -> Unit,
    onPlay: () -> Unit,
    onTalk: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NibbliCard(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Your $streakDays-day streak needs a check-in today.",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onFeed) { Text("Feed") }
                TextButton(onClick = onPlay) { Text("Play") }
                TextButton(onClick = onTalk) { Text("Talk") }
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        }
    }
}
