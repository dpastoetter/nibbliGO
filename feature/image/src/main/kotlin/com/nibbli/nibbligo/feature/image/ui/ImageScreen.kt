package com.nibbli.nibbligo.feature.image.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
        UnsupportedState("image understanding", modifier)
        return
    }
    if (uiState.isLoading) {
        LoadingState(modifier, "Analyzing on-device…")
        return
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Ask Image", style = MaterialTheme.typography.displaySmall)
        Text(
            "Pick a photo from gallery or enter a URI. Requires a vision-capable model.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
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
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = { Text("Question") },
        )
        Button(
            onClick = { viewModel.setImageUri("content://demo/gallery/photo") },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Use demo image") }
        Button(onClick = { viewModel.analyze() }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Analyze on-device")
        }
        uiState.result?.let {
            NibbliCard(modifier = Modifier.padding(top = 16.dp)) {
                Text(it)
            }
        }
    }
}
