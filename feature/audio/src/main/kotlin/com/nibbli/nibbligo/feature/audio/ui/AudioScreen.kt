package com.nibbli.nibbligo.feature.audio.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nibbli.nibbligo.feature.audio.presentation.AudioViewModel

@Composable
fun AudioScribeScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = hiltViewModel(),
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Audio Scribe", style = MaterialTheme.typography.displaySmall)
        Text(
            "Audio transcription requires a multimodal LiteRT model — not yet supported in this build.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
