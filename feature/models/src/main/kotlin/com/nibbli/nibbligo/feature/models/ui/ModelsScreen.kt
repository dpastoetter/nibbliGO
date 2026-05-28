package com.nibbli.nibbligo.feature.models.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.nibbli.nibbligo.core.designsystem.component.OnDeviceBadge
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
        LoadingState(modifier)
        return
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Models", style = MaterialTheme.typography.displaySmall)
        OnDeviceBadge(Modifier.padding(vertical = 8.dp))
        uiState.message?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.models, key = { it.info.id }) { item ->
                NibbliCard {
                    Text(item.info.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(item.info.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
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
                    if (item.isInstalled) {
                        Text(
                            "Installed",
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .testTag("installed_${item.info.id}"),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        OutlinedButton(
                            onClick = { viewModel.uninstall(item.info.id) },
                            modifier = Modifier.padding(top = 8.dp).testTag("uninstall_${item.info.id}"),
                        ) { Text("Remove") }
                    } else {
                        Button(
                            onClick = { viewModel.install(item.info.id) },
                            modifier = Modifier.padding(top = 12.dp).testTag("install_${item.info.id}"),
                        ) { Text("Install") }
                    }
                }
            }
        }
    }
}
