package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nibbli.nibbligo.feature.pet.ui.minigame.PetMinigameHub
import com.nibbli.nibbligo.feature.pet.ui.minigame.PetMinigameId
import com.nibbli.nibbligo.feature.pet.ui.minigame.RetroArcadeShell
import com.nibbli.nibbligo.feature.pet.ui.minigame.SnackDropGame
import com.nibbli.nibbligo.feature.pet.ui.minigame.TidyTapGame

@Composable
fun PetMinigameDialog(
    visible: Boolean,
    dailyTargetScore: Int? = null,
    ghostChallengeScore: Int? = null,
    onDismiss: () -> Unit,
    onWin: () -> Unit,
    onGameEnd: (score: Int, bestCombo: Int, won: Boolean) -> Unit = { _, _, _ -> },
) {
    if (!visible) return

    var selectedGame by rememberSaveable { mutableStateOf<PetMinigameId?>(null) }

    Dialog(
        onDismissRequest = {
            selectedGame = null
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true,
        ),
    ) {
        RetroArcadeShell {
            when (val game = selectedGame) {
                null -> PetMinigameHub(
                    dailyTargetScore = dailyTargetScore,
                    onSelect = { selectedGame = it },
                    onDismiss = {
                        selectedGame = null
                        onDismiss()
                    },
                )
                PetMinigameId.SNACK_DROP -> SnackDropGame(
                    dailyTargetScore = dailyTargetScore,
                    ghostChallengeScore = ghostChallengeScore,
                    onWin = {
                        onWin()
                        selectedGame = null
                        onDismiss()
                    },
                    onDismiss = {
                        selectedGame = null
                        onDismiss()
                    },
                    onGameEnd = onGameEnd,
                    onBack = { selectedGame = null },
                )
                PetMinigameId.TIDY_TAP -> TidyTapGame(
                    dailyTargetScore = dailyTargetScore,
                    ghostChallengeScore = ghostChallengeScore,
                    onWin = {
                        onWin()
                        selectedGame = null
                        onDismiss()
                    },
                    onDismiss = {
                        selectedGame = null
                        onDismiss()
                    },
                    onGameEnd = onGameEnd,
                    onBack = { selectedGame = null },
                )
            }
        }
    }
}
