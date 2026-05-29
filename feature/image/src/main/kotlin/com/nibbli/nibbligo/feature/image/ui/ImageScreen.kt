package com.nibbli.nibbligo.feature.image.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.nibbli.nibbligo.core.ui.LoadingState
import com.nibbli.nibbligo.core.ui.UnsupportedState
import com.nibbli.nibbligo.feature.image.presentation.ImageViewModel

@Composable
fun AskImageScreen(
    modifier: Modifier = Modifier,
    viewModel: ImageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    if (uiState.unsupported) {
        NibbliScreen(modifier = modifier) {
            NibbliScreenHeader(title = "Ask Image")
            UnsupportedState("image understanding", modifier = Modifier.weight(1f))
        }
        return
    }
    if (uiState.isLoading) {
        NibbliScreen(modifier = modifier) {
            LoadingState(message = "Analyzing on-device…")
        }
        return
    }
    NibbliScreen(modifier = modifier, scrollable = true) {
        NibbliScreenHeader(
            title = "Ask Image",
            subtitle = "Pick a photo from gallery or enter a URI. Requires a vision-capable model.",
            showOnDeviceBadge = true,
        )
        OutlinedTextField(
            value = uiState.imageUri,
            onValueChange = viewModel::setImageUri,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Image URI (gallery/camera hook)") },
            placeholder = { Text("content://…") },
        )
        OutlinedTextField(
            value = uiState.question,
            onValueChange = viewModel::setQuestion,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            label = { Text("Question") },
        )
        NibbliPrimaryButton(
            text = "Analyze on-device",
            onClick = { viewModel.analyze() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        uiState.result?.let {
            NibbliCard(modifier = Modifier.padding(top = 16.dp)) {
                Text(it)
            }
        }
    }
}
