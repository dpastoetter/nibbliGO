package com.nibbli.nibbligo.feature.pet.ui.minigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private const val BASKET_WIDTH_FRACTION = 0.28f
private const val CATCH_LINE = 0.84f
private const val TICK_MS = 16L
private const val HITBOX_PAD_FRACTION = 0.012f

private enum class SnackKind(val points: Int) {
    TREAT(1),
    STAR(3),
    BOMB(0),
}

private data class FallingSnack(
    val id: Int,
    val x: Float,
    val y: Float,
    val speed: Float,
    val kind: SnackKind,
    val wobble: Float,
)

@Composable
fun SnackDropGame(
    dailyTargetScore: Int? = null,
    ghostChallengeScore: Int? = null,
    onWin: () -> Unit,
    onDismiss: () -> Unit,
    onGameEnd: (score: Int, bestCombo: Int, won: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val gameId = PetMinigameId.SNACK_DROP
    val durationMs = MinigameBalance.durationMs(gameId)
    val palette = rememberRetroPalette()
    var phase by remember { mutableStateOf(MinigamePhase.READY) }
    var basketX by remember { mutableFloatStateOf(0.5f) }
    var items by remember { mutableStateOf(emptyList<FallingSnack>()) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var nextItemId by remember { mutableIntStateOf(0) }
    var spawnCooldown by remember { mutableIntStateOf(0) }
    var flashText by remember { mutableStateOf<String?>(null) }
    var flashTicks by remember { mutableIntStateOf(0) }

    fun showFlash(text: String) {
        flashText = text
        flashTicks = 45
    }

    fun resetRound() {
        basketX = 0.5f
        items = emptyList()
        score = 0
        lives = 3
        combo = 0
        bestCombo = 0
        elapsedMs = 0L
        nextItemId = 0
        spawnCooldown = 0
        flashText = null
        flashTicks = 0
    }

    LaunchedEffect(phase) {
        if (phase != MinigamePhase.PLAYING) return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        while (phase == MinigamePhase.PLAYING) {
            kotlinx.coroutines.delay(TICK_MS)
            elapsedMs = System.currentTimeMillis() - startedAt
            if (flashTicks > 0) flashTicks -= 1 else flashText = null

            if (elapsedMs >= durationMs) {
                val won = wonByScore(gameId, score, dailyTargetScore, ghostChallengeScore)
                phase = if (won) MinigamePhase.WON else MinigamePhase.LOST
                onGameEnd(score, bestCombo, won)
                break
            }
            if (lives <= 0) {
                phase = MinigamePhase.LOST
                onGameEnd(score, bestCombo, false)
                break
            }

            val difficulty = (elapsedMs / 1_000f).coerceAtMost(18f)
            spawnCooldown -= 1
            if (spawnCooldown <= 0) {
                spawnCooldown = max(12, 52 - (difficulty * 2.2f).toInt())
                val roll = Random.nextFloat()
                val bombRate = if (elapsedMs >= 15_000) 0.10f else 0.11f
                val kind = when {
                    roll < bombRate -> SnackKind.BOMB
                    roll < bombRate + 0.11f -> SnackKind.STAR
                    else -> SnackKind.TREAT
                }
                items = items + FallingSnack(
                    id = nextItemId++,
                    x = Random.nextFloat().coerceIn(0.1f, 0.9f),
                    y = -0.06f,
                    speed = 0.0035f + Random.nextFloat() * 0.0025f + difficulty * 0.00025f,
                    kind = kind,
                    wobble = Random.nextFloat() * 6.28f,
                )
            }

            val basketHalf = BASKET_WIDTH_FRACTION / 2f + HITBOX_PAD_FRACTION
            val basketLeft = basketX - basketHalf
            val basketRight = basketX + basketHalf
            val remaining = mutableListOf<FallingSnack>()

            items.forEach { item ->
                val prevY = item.y
                val newY = item.y + item.speed
                val wobbleX = sin(item.wobble + elapsedMs / 180f) * 0.015f
                val drawX = (item.x + wobbleX).coerceIn(0.05f, 0.95f)

                if (prevY < CATCH_LINE && newY >= CATCH_LINE) {
                    if (drawX in basketLeft..basketRight) {
                        when (item.kind) {
                            SnackKind.TREAT -> {
                                combo += 1
                                bestCombo = max(bestCombo, combo)
                                val bonus = if (combo >= 5) 2 else if (combo >= 3) 1 else 0
                                score += item.kind.points + bonus
                                showFlash(
                                    if (bonus > 0) "+${item.kind.points + bonus} x$combo!" else "+${item.kind.points}",
                                )
                            }
                            SnackKind.STAR -> {
                                combo += 1
                                bestCombo = max(bestCombo, combo)
                                score += item.kind.points + combo.coerceAtMost(4)
                                showFlash("★ +${item.kind.points + combo.coerceAtMost(4)}")
                            }
                            SnackKind.BOMB -> {
                                combo = 0
                                lives -= 1
                                showFlash("BOOM! -1")
                            }
                        }
                        return@forEach
                    } else if (item.kind != SnackKind.BOMB) {
                        combo = 0
                    }
                }

                if (newY > 1.08f) {
                    if (item.kind != SnackKind.BOMB && prevY < CATCH_LINE + 0.08f) {
                        combo = 0
                    }
                    return@forEach
                }

                remaining.add(item.copy(y = newY, wobble = item.wobble + 0.08f))
            }
            items = remaining
        }
    }

    val effectiveDaily = dailyTargetScore?.let { MinigameBalance.effectiveDailyTarget(gameId, it) }
    val subtitle = when (phase) {
        MinigamePhase.READY -> buildString {
            append("Slide basket · catch snacks & stars · dodge bombs")
            effectiveDaily?.let { append(" · goal $it pts") }
            ghostChallengeScore?.let { append(" · beat $it!") }
        }
        MinigamePhase.PLAYING -> "Combos score extra — keep the streak!"
        MinigamePhase.WON -> "High score reflexes! nibbli is thrilled."
        MinigamePhase.LOST -> "So close — one more round?"
    }

    RetroMinigameScaffold(
        title = "SNACK DROP",
        subtitle = subtitle,
        palette = palette,
        hud = if (phase != MinigamePhase.READY) {
            RetroHudState(
                scoreLabel = scoreGoalText(gameId, score, dailyTargetScore, ghostChallengeScore),
                livesLabel = "♥".repeat(lives.coerceAtLeast(0)),
                timeLabel = formatTimeRemaining(durationMs, elapsedMs),
            )
        } else {
            null
        },
        onClose = onDismiss,
        onBack = onBack,
        field = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(phase) {
                        if (phase != MinigamePhase.PLAYING) return@pointerInput
                        detectTapGestures { offset ->
                            basketX = (offset.x / size.width).coerceIn(
                                BASKET_WIDTH_FRACTION / 2f,
                                1f - BASKET_WIDTH_FRACTION / 2f,
                            )
                        }
                    }
                    .pointerInput(phase) {
                        if (phase != MinigamePhase.PLAYING) return@pointerInput
                        detectDragGestures { change, _ ->
                            change.consume()
                            basketX = (change.position.x / size.width).coerceIn(
                                BASKET_WIDTH_FRACTION / 2f,
                                1f - BASKET_WIDTH_FRACTION / 2f,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (phase == MinigamePhase.READY) {
                    RetroReadyPanel(text = "▶ PRESS START", palette = palette)
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val px = fieldPixelUnit()
                        drawRetroGrid(palette, px)
                        val w = size.width
                        val h = size.height

                        items.forEach { item ->
                            val wobbleX = sin(item.wobble + elapsedMs / 180f) * 0.015f
                            val cx = (item.x + wobbleX).coerceIn(0.05f, 0.95f) * w
                            val cy = item.y * h
                            when (item.kind) {
                                SnackKind.TREAT -> drawPixelTreat(palette, cx, cy, px)
                                SnackKind.STAR -> drawPixelStar(palette, cx, cy, px)
                                SnackKind.BOMB -> drawPixelBomb(palette, cx, cy, px)
                            }
                        }

                        val basketW = w * BASKET_WIDTH_FRACTION
                        val basketH = px * 2.2f
                        val basketLeft = basketX * w - basketW / 2f
                        val basketTop = h * CATCH_LINE - basketH / 2f
                        drawRoundRect(
                            color = palette.accent,
                            topLeft = Offset(basketLeft, basketTop),
                            size = Size(basketW, basketH),
                            cornerRadius = CornerRadius(px * 0.25f, px * 0.25f),
                        )
                        drawRoundRect(
                            color = palette.pixel,
                            topLeft = Offset(basketLeft + px * 0.4f, basketTop - px * 0.7f),
                            size = Size(basketW - px * 0.8f, px * 0.8f),
                            cornerRadius = CornerRadius(px * 0.15f, px * 0.15f),
                        )
                        drawLine(
                            color = palette.lcdDark.copy(alpha = 0.5f),
                            start = Offset(0f, h * CATCH_LINE),
                            end = Offset(w, h * CATCH_LINE),
                            strokeWidth = px * 0.25f,
                        )
                    }
                    flashText?.let {
                        RetroFlashText(
                            text = it,
                            palette = palette,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 36.dp),
                        )
                    }
                }
            }
        },
        footer = {
            when (phase) {
                MinigamePhase.READY -> {
                    RetroPixelButton(
                        text = "Start",
                        onClick = {
                            resetRound()
                            phase = MinigamePhase.PLAYING
                        },
                        modifier = Modifier.fillMaxWidth(),
                        palette = palette,
                    )
                }
                MinigamePhase.PLAYING -> {
                    RetroFooterText(
                        text = if (combo >= 3) "COMBO x$combo" else "DRAG ANYWHERE",
                        palette = palette,
                        accent = true,
                    )
                }
                MinigamePhase.WON -> {
                    RetroFooterText(text = "FINAL $score · BEST COMBO x$bestCombo", palette = palette)
                    RetroPixelButton(
                        text = "Claim reward",
                        onClick = onWin,
                        modifier = Modifier.fillMaxWidth(),
                        palette = palette,
                    )
                }
                MinigamePhase.LOST -> {
                    RetroFooterText(text = "SCORE $score · COMBO x$bestCombo", palette = palette)
                    RetroPixelButton(
                        text = "Try again",
                        onClick = {
                            resetRound()
                            phase = MinigamePhase.PLAYING
                        },
                        modifier = Modifier.fillMaxWidth(),
                        palette = palette,
                    )
                }
            }
        },
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelTreat(
    palette: RetroPalette,
    cx: Float,
    cy: Float,
    px: Float,
) {
    val s = px * 0.65f
    drawPixelRect(palette.highlight, cx - s, cy - s, s * 2, s * 2)
    drawPixelRect(palette.pixel, cx - s * 0.4f, cy - px * 0.25f, px * 0.4f, px * 0.4f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelStar(
    palette: RetroPalette,
    cx: Float,
    cy: Float,
    px: Float,
) {
    val arm = px * 0.75f
    drawPixelRect(palette.highlight, cx - px * 0.15f, cy - arm, px * 0.3f, arm * 2)
    drawPixelRect(palette.highlight, cx - arm, cy - px * 0.15f, arm * 2, px * 0.3f)
    drawPixelRect(palette.highlight, cx - arm * 0.65f, cy - arm * 0.65f, arm * 1.3f, arm * 1.3f)
    drawPixelRect(Color.White.copy(alpha = 0.6f), cx - px * 0.15f, cy - px * 0.5f, px * 0.3f, px * 0.3f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPixelBomb(
    palette: RetroPalette,
    cx: Float,
    cy: Float,
    px: Float,
) {
    drawPixelCircle(palette.danger, cx, cy, px * 0.9f)
    drawPixelRect(palette.pixel, cx - px * 0.15f, cy - px * 1.4f, px * 0.3f, px * 0.55f)
    val d = px * 0.45f
    drawLine(Color.White, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeWidth = px * 0.25f)
    drawLine(Color.White, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeWidth = px * 0.25f)
}
