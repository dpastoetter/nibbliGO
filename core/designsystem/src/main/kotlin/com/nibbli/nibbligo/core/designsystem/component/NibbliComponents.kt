package com.nibbli.nibbligo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.theme.OnDeviceGreen
import kotlinx.coroutines.delay

@Composable
fun NibbliCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun StatBar(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("$value", style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { value / 100f },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
fun OnDeviceBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = OnDeviceGreen.copy(alpha = 0.2f),
    ) {
        Text(
            text = if (compact) "On-device" else "Processed on-device",
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp,
            ),
            style = if (compact) {
                MaterialTheme.typography.labelSmall
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = OnDeviceGreen,
        )
    }
}

@Composable
fun PetBubble(
    text: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    @Suppress("UNUSED_PARAMETER") bubbleColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    applyHorizontalInset: Boolean = true,
) {
    val displayText = if (isLoading) LoadingDots() else text
    val insetModifier = if (applyHorizontalInset) Modifier.padding(horizontal = 16.dp) else Modifier
    NibbliMessageBubble(
        text = displayText,
        role = NibbliMessageRole.ASSISTANT,
        modifier = modifier.then(insetModifier),
        maxLines = 5,
    )
}

@Composable
private fun LoadingDots(): String {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            frame = (frame + 1) % 4
        }
    }
    return when (frame) {
        0 -> "…"
        1 -> ".  "
        2 -> ".. "
        else -> "..."
    }
}

@Composable
fun ModelCapabilityChip(label: String, modifier: Modifier = Modifier) {
    NibbliSuggestionChip(
        label = label,
        onClick = {},
        modifier = modifier,
        enabled = false,
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    description: String,
    preview: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Text(
                    preview,
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_action"),
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
