package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun PetCelebrationSheet(
    event: PetCelebrationEvent?,
    petName: String,
    onDismiss: () -> Unit,
    onEquipNow: () -> Unit = {},
) {
    event ?: return
    val (title, body) = when (event) {
        PetCelebrationEvent.DailyQuestComplete -> {
            "Daily quest complete!" to
                "$petName is thrilled! Mood and trust boosted, and a new floor prop may be waiting in Items."
        }
        is PetCelebrationEvent.ItemUnlocked -> {
            val kindLabel = when (event.kind) {
                PetCelebrationEvent.UnlockKind.COSMETIC -> "wearable"
                PetCelebrationEvent.UnlockKind.SCENE -> "scene"
                PetCelebrationEvent.UnlockKind.PROP -> "floor prop"
            }
            "New $kindLabel unlocked!" to buildString {
                append("You unlocked ${event.displayName}. ")
                append("Open the care menu → Items → ● to equip.")
                event.hint?.let { append("\n\nUnlock tip: $it") }
            }
        }
    }
    val showEquip = event is PetCelebrationEvent.ItemUnlocked
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Nice!")
            }
        },
        dismissButton = if (showEquip) {
            {
                TextButton(onClick = onEquipNow) {
                    Text("Equip now")
                }
            }
        } else {
            null
        },
    )
}
