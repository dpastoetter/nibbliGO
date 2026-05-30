package com.nibbli.nibbligo.feature.models.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.ModelCapabilityChip
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.ui.LoadingState
import com.nibbli.nibbligo.feature.models.presentation.ModelsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier,
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.isLoading) {
        NibbliScreen(modifier = modifier) {
            LoadingState(message = "Loading models…")
        }
        return
    }
    NibbliScreen(modifier = modifier) {
        NibbliScreenHeader(
            title = "Models",
            subtitle = "Download and manage on-device LiteRT models.",
            showOnDeviceBadge = true,
        )
        if (uiState.models.any { it.info.requiresHfAuth } && !uiState.hfSignedIn) {
            Text(
                "SmolLM2 360M installs without sign-in and is a great Pixel Friend model. " +
                    "Gemma 3 and FunctionGemma are gated — sign in or paste a token in Settings, " +
                    "accept each model license on huggingface.co, then install.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        uiState.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.models, key = { it.info.id }) { item ->
                NibbliCard {
                    Text(item.info.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        item.info.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (item.info.requiresHfAuth && !uiState.hfSignedIn) {
                        Text(
                            "Requires Hugging Face sign-in",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Text(
                        "Size: ${item.info.sizeBytes / 1_000_000} MB · RAM ~${item.info.estimatedRamMb} MB",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item.info.modalities.forEach { mod ->
                            ModelCapabilityChip(viewModel.modalityLabel(mod))
                        }
                    }
                    if (item.isWaitingForNetwork) {
                        Text(
                            "Waiting for network…",
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else if (item.isDownloading) {
                        val progressLabel = if (item.downloadProgress > 0) {
                            "Downloading… ${item.downloadProgress}%"
                        } else {
                            "Downloading…"
                        }
                        Text(
                            progressLabel,
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else if (item.isInstalled) {
                        Text(
                            "Installed",
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag("installed_${item.info.id}"),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        NibbliPrimaryButton(
                            text = "Remove",
                            onClick = { viewModel.uninstall(item.info.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("uninstall_${item.info.id}"),
                        )
                    } else if (!item.isDownloading) {
                        NibbliPrimaryButton(
                            text = "Install",
                            onClick = { viewModel.install(item.info.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag("install_${item.info.id}"),
                        )
                    }
                }
            }
        }
    }
}
