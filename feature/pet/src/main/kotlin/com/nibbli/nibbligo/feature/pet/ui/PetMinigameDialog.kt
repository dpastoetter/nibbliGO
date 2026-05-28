package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun PetMinigameDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onWin: () -> Unit,
) {
    if (!visible) return
    var target by remember { mutableIntStateOf(Random.nextInt(1, 6)) }
    var guess by remember { mutableIntStateOf(3) }
    var message by remember { mutableStateOf("Guess the number 1–5!") }
    var won by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catch game") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (1..5).forEach { n ->
                        Button(onClick = {
                            guess = n
                            if (n == target) {
                                message = "You got it!"
                                won = true
                            } else {
                                message = "Nope! Try again."
                                target = Random.nextInt(1, 6)
                            }
                        }) { Text("$n") }
                    }
                }
            }
        },
        confirmButton = {
            if (won) {
                Button(onClick = { onWin(); onDismiss() }) { Text("Yay!") }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
    )
}
