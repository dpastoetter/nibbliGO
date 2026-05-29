package com.nibbli.nibbligo.feature.promptlab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.core.designsystem.component.NibbliSuggestionChip
import com.nibbli.nibbligo.core.model.PromptPreset
import com.nibbli.nibbligo.feature.promptlab.presentation.PromptLabViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PromptLabScreen(
    modifier: Modifier = Modifier,
    viewModel: PromptLabViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "Prompt Lab",
            subtitle = "Try presets and compare on-device model outputs.",
            showOnDeviceBadge = true,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PromptPreset.entries.forEach { preset ->
                NibbliSuggestionChip(
                    label = preset.label,
                    selected = uiState.preset == preset,
                    onClick = { viewModel.selectPreset(preset) },
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NibbliPrimaryButton(
                text = if (uiState.isRunning) "Running…" else "Run",
                onClick = { viewModel.run() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRunning,
            )
            NibbliPrimaryButton(
                text = "Compare models",
                onClick = { viewModel.compareAll() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRunning,
            )
            NibbliPrimaryButton(
                text = "Save favorite",
                onClick = { viewModel.saveFavorite() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (uiState.output.isNotBlank()) {
            NibbliCard(modifier = Modifier.padding(top = 8.dp)) {
                Text("Output", style = MaterialTheme.typography.titleMedium)
                Text(uiState.output, modifier = Modifier.padding(top = 8.dp))
            }
        }
        uiState.compareResults.forEach { result ->
            NibbliCard(modifier = Modifier.padding(top = 8.dp)) {
                Text(result.modelId, style = MaterialTheme.typography.titleMedium)
                Text(result.output, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
