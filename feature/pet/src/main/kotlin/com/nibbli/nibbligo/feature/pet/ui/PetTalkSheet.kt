package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PetTalkSheet(
    visible: Boolean,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val chips = listOf("How are you?", "I'm back!", "Good night", "Let's play!")

    LaunchedEffect(visible) {
        if (!visible) text = ""
    }

    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Talk to nibbli")
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Say something…") },
                enabled = !isGenerating,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { chip ->
                    Button(onClick = { text = chip }, enabled = !isGenerating) {
                        Text(chip)
                    }
                }
            }
            Button(
                onClick = {
                    val message = text.trim()
                    if (message.isEmpty() || isGenerating) return@Button
                    text = ""
                    scope.launch {
                        onSend(message)
                        sheetState.hide()
                        onDismiss()
                    }
                },
                enabled = text.isNotBlank() && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isGenerating) "Thinking…" else "Send")
            }
        }
    }
}
