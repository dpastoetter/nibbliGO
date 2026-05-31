package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val modelId = ModelCatalog.RECOMMENDED_PET_MODEL_ID
    val modelName = ModelCatalog.displayName(modelId)
    val sizeLabel = ModelCatalog.approximateSizeLabel(modelId)
    NibbliCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Install $modelName to unlock Talk",
                style = MaterialTheme.typography.titleSmall,
            )
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
