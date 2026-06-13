package com.nibbli.nibbligo.feature.audio.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliComingSoonCard
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
        NibbliComingSoonCard(
            title = "Audio transcription",
            body = "Audio Scribe will transcribe and summarize recordings with a multimodal LiteRT model. " +
                "It is not available in this build yet.",
            modifier = Modifier.padding(bottom = 12.dp),
        )
        NibbliCard {
            Text(
                "Record or import audio, then get a transcript and summary — all on your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
