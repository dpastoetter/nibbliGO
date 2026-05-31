package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliSecondaryButton
import com.nibbli.nibbligo.core.model.ModelCatalog

@Composable
fun PetModelSetupSheet(
    visible: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    message: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onClearMessage: () -> Unit,
) {
    if (!visible) return
    val modelId = ModelCatalog.RECOMMENDED_PET_MODEL_ID
    val modelName = ModelCatalog.displayName(modelId)
    val sizeLabel = ModelCatalog.approximateSizeLabel(modelId)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Download Pixel Friend brain")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Talk, voice, and mood lines need an on-device model ($sizeLabel). " +
                        "$modelName runs locally — no sign-in required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { (downloadProgress.coerceIn(0, 100) / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    Text(
                        text = if (downloadProgress > 0) {
                            "Downloading… $downloadProgress%"
                        } else {
                            "Starting download…"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            NibbliPrimaryButton(
                text = if (isDownloading) "Downloading…" else "Download",
                onClick = {
                    onClearMessage()
                    onDownload()
                },
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        },
        dismissButton = {
            NibbliSecondaryButton(
                text = "Later",
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            )
        },
    )
}

@Composable
fun PetModelSetupBanner(
    visible: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    onDownload: () -> Unit,
    startCollapsed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val modelId = ModelCatalog.RECOMMENDED_PET_MODEL_ID
    val modelName = ModelCatalog.displayName(modelId)
    val sizeLabel = ModelCatalog.approximateSizeLabel(modelId)

    var expanded by rememberSaveable { mutableStateOf(!startCollapsed) }
    LaunchedEffect(startCollapsed) {
        if (startCollapsed) expanded = false
    }
    LaunchedEffect(isDownloading) {
        if (isDownloading) expanded = true
    }

    val toggleDescription = if (expanded) "Collapse install model card" else "Expand install model card"
    val subtitle = when {
        isDownloading && downloadProgress > 0 -> "Downloading… $downloadProgress%"
        isDownloading -> "Starting download…"
        else -> "Tap to download ($sizeLabel)"
    }

    NibbliCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .semantics { contentDescription = toggleDescription }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = "Install $modelName to unlock Talk",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (!expanded) {
                        Text(
                            text = subtitle,
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

            if (!expanded && isDownloading) {
                LinearProgressIndicator(
                    progress = { (downloadProgress.coerceIn(0, 100) / 100f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "On-device chat for your Pixel Friend — runs entirely on your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { (downloadProgress.coerceIn(0, 100) / 100f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                        Text(
                            text = if (downloadProgress > 0) "Downloading… $downloadProgress%" else "Starting…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        NibbliPrimaryButton(
                            text = "Download ($sizeLabel)",
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
