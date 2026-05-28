package com.nibbli.nibbligo.feature.audio.ui

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
import com.nibbli.nibbligo.feature.audio.presentation.AudioViewModel

@Composable
fun AudioScribeScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Audio Scribe", style = MaterialTheme.typography.displaySmall)
        Text(
            "Recordings and transcripts stay on your device unless you export.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Button(onClick = { viewModel.simulateRecord() }) { Text("Simulate record") }
        Button(onClick = { viewModel.transcribeLatest(false) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Transcribe latest")
        }
        Button(onClick = { viewModel.transcribeLatest(true) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Transcribe & summarize")
        }
        uiState.status?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(uiState.recordings) { rec ->
                NibbliCard(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text("Recording ${rec.id}", style = MaterialTheme.typography.titleMedium)
                    Text(rec.uri, style = MaterialTheme.typography.bodySmall)
                    rec.transcript?.let { Text(it, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }
    }
}
