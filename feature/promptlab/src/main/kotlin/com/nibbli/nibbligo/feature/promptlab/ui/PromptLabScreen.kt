package com.nibbli.nibbligo.feature.promptlab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.model.PromptPreset
import com.nibbli.nibbligo.feature.promptlab.presentation.PromptLabViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptLabScreen(
    modifier: Modifier = Modifier,
    viewModel: PromptLabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Prompt Lab", style = MaterialTheme.typography.displaySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PromptPreset.entries.forEach { preset ->
                FilterChip(
                    selected = uiState.preset == preset,
                    onClick = { viewModel.selectPreset(preset) },
                    label = { Text(preset.label) },
                )
            }
        }
        OutlinedTextField(
            value = uiState.prompt,
            onValueChange = viewModel::updatePrompt,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Prompt") },
            minLines = 4,
        )
        RowButtons(viewModel, uiState.isRunning)
        if (uiState.output.isNotBlank()) {
            NibbliCard {
                Text("Output", style = MaterialTheme.typography.titleMedium)
                Text(uiState.output, modifier = Modifier.padding(top = 8.dp))
            }
        }
        uiState.compareResults.forEach { result ->
            NibbliCard {
                Text(result.modelId, style = MaterialTheme.typography.titleMedium)
                Text(result.output, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun RowButtons(viewModel: PromptLabViewModel, isRunning: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { viewModel.run() }, enabled = !isRunning) { Text("Run") }
        Button(onClick = { viewModel.compareAll() }, enabled = !isRunning) { Text("Compare models") }
        Button(onClick = { viewModel.saveFavorite() }) { Text("Save favorite") }
    }
}
