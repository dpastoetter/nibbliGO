package com.nibbli.nibbligo.feature.benchmark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import com.nibbli.nibbligo.feature.benchmark.presentation.BenchmarkViewModel

@Composable
fun BenchmarkScreen(
    modifier: Modifier = Modifier,
    viewModel: BenchmarkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NibbliScreen(modifier = modifier) {
        NibbliScreenHeader(
            title = "Benchmark",
            subtitle = "Measure on-device performance per model.",
            showOnDeviceBadge = true,
        )
        uiState.installedModelIds.forEach { id ->
            NibbliPrimaryButton(
                text = "Run: $id",
                onClick = {
                    viewModel.selectModel(id)
                    viewModel.runBenchmark()
                },
                enabled = !uiState.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
        uiState.lastResult?.let { Text(it, modifier = Modifier.padding(bottom = 12.dp)) }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.history) { run ->
                NibbliCard {
                    Text(run.modelId, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "TTFT ${run.metrics.timeToFirstTokenMs}ms · ${run.metrics.tokensPerSecond} tok/s",
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        run.metrics.thermalNote,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
