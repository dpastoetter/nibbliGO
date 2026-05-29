package com.nibbli.nibbligo.feature.audio.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreen
import com.nibbli.nibbligo.core.designsystem.component.NibbliScreenHeader
import com.nibbli.nibbligo.feature.audio.presentation.AudioViewModel

@Composable
fun AudioScribeScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = hiltViewModel(),
) {
    NibbliScreen(modifier = modifier) {
        NibbliScreenHeader(
            title = "Audio Scribe",
            subtitle = "Transcribe and summarize recordings on-device.",
            showOnDeviceBadge = true,
        )
        NibbliCard {
            Text(
                "Audio transcription requires a multimodal LiteRT model — not yet supported in this build.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
