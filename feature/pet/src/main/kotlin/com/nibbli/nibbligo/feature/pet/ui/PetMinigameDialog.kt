package com.nibbli.nibbligo.feature.pet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nibbli.nibbligo.core.designsystem.component.NibbliCard
import com.nibbli.nibbligo.core.designsystem.component.NibbliPrimaryButton
import kotlin.math.max
import kotlin.random.Random

private const val GAME_DURATION_MS = 28_000L
private const val WIN_SCORE = 18
private const val BASKET_WIDTH_FRACTION = 0.24f
private const val CATCH_LINE = 0.84f
private const val TICK_MS = 16L

private enum class CatchItemKind(val points: Int) {
    TREAT(1),
    STAR(3),
    BOMB(0),
}

private enum class GamePhase {
    READY,
    PLAYING,
    WON,
    LOST,
}

private data class FallingItem(
    val id: Int,
    val x: Float,
    val y: Float,
    val speed: Float,
    val kind: CatchItemKind,
    val wobble: Float,
)

@Composable
fun PetMinigameDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onWin: () -> Unit,
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        NibbliCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            TreatCatchGame(
                onWin = {
                    onWin()
                    onDismiss()
                },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun TreatCatchGame(
    onWin: () -> Unit,
    onDismiss: () -> Unit,
) {
    var phase by remember { mutableStateOf(GamePhase.READY) }
    var basketX by remember { mutableFloatStateOf(0.5f) }
    var items by remember { mutableStateOf(emptyList<FallingItem>()) }
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
        if (phase != GamePhase.PLAYING) return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        while (phase == GamePhase.PLAYING) {
            kotlinx.coroutines.delay(TICK_MS)
            elapsedMs = System.currentTimeMillis() - startedAt
            if (flashTicks > 0) flashTicks -= 1 else flashText = null

            if (elapsedMs >= GAME_DURATION_MS) {
                phase = if (score >= WIN_SCORE) GamePhase.WON else GamePhase.LOST
                break
            }
            if (lives <= 0) {
                phase = GamePhase.LOST
                break
            }

            val difficulty = (elapsedMs / 1_000f).coerceAtMost(18f)
            spawnCooldown -= 1
            if (spawnCooldown <= 0) {
                spawnCooldown = max(12, 52 - (difficulty * 2.2f).toInt())
                val roll = Random.nextFloat()
                val kind = when {
                    roll < 0.11f -> CatchItemKind.BOMB
                    roll < 0.22f -> CatchItemKind.STAR
                    else -> CatchItemKind.TREAT
                }
                items = items + FallingItem(
                    id = nextItemId++,
                    x = Random.nextFloat().coerceIn(0.1f, 0.9f),
                    y = -0.06f,
                    speed = 0.0035f + Random.nextFloat() * 0.0025f + difficulty * 0.00025f,
                    kind = kind,
                    wobble = Random.nextFloat() * 6.28f,
                )
            }

            val basketHalf = BASKET_WIDTH_FRACTION / 2f
            val basketLeft = basketX - basketHalf
            val basketRight = basketX + basketHalf
            val remaining = mutableListOf<FallingItem>()

            items.forEach { item ->
                val prevY = item.y
                val newY = item.y + item.speed
                val wobbleX = kotlin.math.sin(item.wobble + elapsedMs / 180f) * 0.015f
                val drawX = (item.x + wobbleX).coerceIn(0.05f, 0.95f)

                if (prevY < CATCH_LINE && newY >= CATCH_LINE) {
                    if (drawX in basketLeft..basketRight) {
                        when (item.kind) {
                            CatchItemKind.TREAT -> {
                                combo += 1
                                bestCombo = max(bestCombo, combo)
                                val bonus = if (combo >= 5) 2 else if (combo >= 3) 1 else 0
                                score += item.kind.points + bonus
                                showFlash(
                                    if (bonus > 0) "+${item.kind.points + bonus} x$combo combo!" else "+${item.kind.points}",
                                )
                            }
                            CatchItemKind.STAR -> {
                                combo += 1
                                bestCombo = max(bestCombo, combo)
                                score += item.kind.points + combo.coerceAtMost(4)
                                showFlash("Star! +${item.kind.points + combo.coerceAtMost(4)}")
                            }
                            CatchItemKind.BOMB -> {
                                combo = 0
                                lives -= 1
                                showFlash("Ouch! -1 life")
                            }
                        }
                        return@forEach
                    } else if (item.kind != CatchItemKind.BOMB) {
                        combo = 0
                    }
                }

                if (newY > 1.08f) {
                    if (item.kind != CatchItemKind.BOMB && prevY < CATCH_LINE + 0.08f) {
                        combo = 0
                    }
                    return@forEach
                }

                remaining.add(item.copy(y = newY, wobble = item.wobble + 0.08f))
            }
            items = remaining
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Treat catch!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = when (phase) {
                GamePhase.READY -> "Drag the basket. Catch snacks and stars, dodge bombs!"
                GamePhase.PLAYING -> "Keep the streak alive — combos score extra!"
                GamePhase.WON -> "Amazing reflexes! nibbli is thrilled."
                GamePhase.LOST -> "So close! Want another round?"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (phase == GamePhase.PLAYING || phase == GamePhase.WON || phase == GamePhase.LOST) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatPill(label = "Score", value = "$score / $WIN_SCORE")
                StatPill(label = "Lives", value = "❤".repeat(lives.coerceAtLeast(0)))
                StatPill(
                    label = "Time",
                    value = "${((GAME_DURATION_MS - elapsedMs.coerceAtMost(GAME_DURATION_MS)) / 1000).toInt()}s",
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                )
                .pointerInput(phase) {
                    if (phase != GamePhase.PLAYING) return@pointerInput
                    detectTapGestures { offset ->
                        basketX = (offset.x / size.width).coerceIn(
                            BASKET_WIDTH_FRACTION / 2f,
                            1f - BASKET_WIDTH_FRACTION / 2f,
                        )
                    }
                }
                .pointerInput(phase) {
                    if (phase != GamePhase.PLAYING) return@pointerInput
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
            if (phase == GamePhase.READY) {
                Text(
                    text = "🧺 Ready?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            } else {
                val treatColor = MaterialTheme.colorScheme.primary
                val starColor = Color(0xFFFFB300)
                val bombColor = MaterialTheme.colorScheme.error
                val basketColor = MaterialTheme.colorScheme.tertiary

                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    items.forEach { item ->
                        val wobbleX = kotlin.math.sin(item.wobble + elapsedMs / 180f) * 0.015f
                        val cx = (item.x + wobbleX).coerceIn(0.05f, 0.95f) * w
                        val cy = item.y * h
                        val radius = when (item.kind) {
                            CatchItemKind.STAR -> 12f
                            CatchItemKind.BOMB -> 11f
                            CatchItemKind.TREAT -> 10f
                        }
                        val color = when (item.kind) {
                            CatchItemKind.STAR -> starColor
                            CatchItemKind.BOMB -> bombColor
                            CatchItemKind.TREAT -> treatColor
                        }
                        drawCircle(color = color, radius = radius, center = Offset(cx, cy))
                        if (item.kind == CatchItemKind.STAR) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.55f),
                                radius = 3.5f,
                                center = Offset(cx - 3f, cy - 3f),
                            )
                        }
                        if (item.kind == CatchItemKind.BOMB) {
                            drawLine(
                                color = Color.White,
                                start = Offset(cx - 4f, cy - 4f),
                                end = Offset(cx + 4f, cy + 4f),
                                strokeWidth = 2f,
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(cx + 4f, cy - 4f),
                                end = Offset(cx - 4f, cy + 4f),
                                strokeWidth = 2f,
                            )
                        }
                    }

                    val basketW = w * BASKET_WIDTH_FRACTION
                    val basketH = 22f
                    val basketLeft = basketX * w - basketW / 2f
                    val basketTop = h * CATCH_LINE - basketH / 2f
                    drawRoundRect(
                        color = basketColor,
                        topLeft = Offset(basketLeft, basketTop),
                        size = Size(basketW, basketH),
                        cornerRadius = CornerRadius(8f, 8f),
                    )
                    drawRoundRect(
                        color = basketColor.copy(alpha = 0.35f),
                        topLeft = Offset(basketLeft + 4f, basketTop - 6f),
                        size = Size(basketW - 8f, 8f),
                        cornerRadius = CornerRadius(4f, 4f),
                    )

                    drawLine(
                        color = Color.White.copy(alpha = 0.25f),
                        start = Offset(0f, h * CATCH_LINE),
                        end = Offset(w, h * CATCH_LINE),
                        strokeWidth = 1.5f,
                    )
                }

                flashText?.let { msg ->
                    Text(
                        text = msg,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        when (phase) {
            GamePhase.READY -> {
                NibbliPrimaryButton(
                    text = "Start!",
                    onClick = {
                        resetRound()
                        phase = GamePhase.PLAYING
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GamePhase.PLAYING -> {
                Text(
                    text = if (combo >= 3) "Combo x$combo!" else "Drag anywhere on the field",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            GamePhase.WON -> {
                Text(
                    text = "Final score $score · Best combo x$bestCombo",
                    style = MaterialTheme.typography.bodySmall,
                )
                NibbliPrimaryButton(
                    text = "Claim reward",
                    onClick = onWin,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GamePhase.LOST -> {
                Text(
                    text = "Score $score · Best combo x$bestCombo",
                    style = MaterialTheme.typography.bodySmall,
                )
                NibbliPrimaryButton(
                    text = "Try again",
                    onClick = {
                        resetRound()
                        phase = GamePhase.PLAYING
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        TextButton(onClick = onDismiss) {
            Text("Close")
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
