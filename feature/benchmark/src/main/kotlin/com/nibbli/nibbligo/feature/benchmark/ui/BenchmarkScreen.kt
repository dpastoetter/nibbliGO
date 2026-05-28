package com.nibbli.nibbligo.feature.benchmark.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.feature.benchmark.presentation.BenchmarkViewModel

@Composable
fun BenchmarkScreen(
    modifier: Modifier = Modifier,
    viewModel: BenchmarkViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Benchmark", style = MaterialTheme.typography.displaySmall)
        Text("Measure on-device performance per model.", modifier = Modifier.padding(vertical = 8.dp))
        uiState.installedModelIds.forEach { id ->
            Button(
                onClick = { viewModel.selectModel(id); viewModel.runBenchmark() },
                enabled = !uiState.isRunning,
                modifier = Modifier.padding(bottom = 8.dp),
            ) { Text("Run: $id") }
        }
        uiState.lastResult?.let { Text(it, modifier = Modifier.padding(bottom = 12.dp)) }
        LazyColumn {
            items(uiState.history) { run ->
                NibbliCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(run.modelId, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "TTFT ${run.metrics.timeToFirstTokenMs}ms · ${run.metrics.tokensPerSecond} tok/s",
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(run.metrics.thermalNote, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
