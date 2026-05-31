package com.nibbli.nibbligo.feature.pet.ui.minigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private const val GRID = 3
private const val TICK_MS = 16L
private const val DUAL_SPOT_AFTER_MS = 8_000L

private data class TidySpot(
    val cell: Int,
    val expiresAtMs: Long,
    val spawnAtMs: Long,
)

@Composable
fun TidyTapGame(
    dailyTargetScore: Int? = null,
    ghostChallengeScore: Int? = null,
    onWin: () -> Unit,
    onDismiss: () -> Unit,
    onGameEnd: (score: Int, bestCombo: Int, won: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val gameId = PetMinigameId.TIDY_TAP
    val durationMs = MinigameBalance.durationMs(gameId)
    val palette = rememberRetroPalette()
    var phase by remember { mutableStateOf(MinigamePhase.READY) }
    var activeSpots by remember { mutableStateOf(emptyList<TidySpot>()) }
    var score by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var spawnCooldown by remember { mutableIntStateOf(0) }
    var flashText by remember { mutableStateOf<String?>(null) }
    var flashTicks by remember { mutableIntStateOf(0) }
    var missShakeTicks by remember { mutableIntStateOf(0) }

    fun showFlash(text: String) {
        flashText = text
        flashTicks = 40
    }

    fun resetRound() {
        activeSpots = emptyList()
        score = 0
        misses = 0
        combo = 0
        bestCombo = 0
        elapsedMs = 0L
        spawnCooldown = 20
        flashText = null
        flashTicks = 0
        missShakeTicks = 0
    }

    fun tapCell(cell: Int) {
        val spot = activeSpots.find { it.cell == cell }
        if (spot != null) {
            combo += 1
            bestCombo = max(bestCombo, combo)
            val bonus = when {
                combo >= 6 -> 2
                combo >= 3 -> 1
                else -> 0
            }
            score += 1 + bonus
            showFlash("CLEAN +${1 + bonus}")
            activeSpots = activeSpots.filter { it.cell != cell }
            spawnCooldown = max(8, 22 - combo)
        } else if (activeSpots.isNotEmpty()) {
            combo = 0
            missShakeTicks = 12
            showFlash("MISS!")
        }
    }

    fun spawnSpot(now: Long, difficulty: Float, occupied: Set<Int>): TidySpot? {
        val freeCells = (0 until GRID * GRID).filter { it !in occupied }
        if (freeCells.isEmpty()) return null
        val lifetime = (1_100 - difficulty * 35).toLong().coerceIn(700, 1_100)
        return TidySpot(
            cell = freeCells.random(),
            expiresAtMs = now + lifetime,
            spawnAtMs = now,
        )
    }

    LaunchedEffect(phase) {
        if (phase != MinigamePhase.PLAYING) return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        while (phase == MinigamePhase.PLAYING) {
            kotlinx.coroutines.delay(TICK_MS)
            val now = System.currentTimeMillis()
            elapsedMs = now - startedAt
            if (flashTicks > 0) flashTicks -= 1 else flashText = null
            if (missShakeTicks > 0) missShakeTicks -= 1

            if (elapsedMs >= durationMs) {
                val won = wonByScore(gameId, score, dailyTargetScore, ghostChallengeScore)
                phase = if (won) MinigamePhase.WON else MinigamePhase.LOST
                onGameEnd(score, bestCombo, won)
                break
            }
            if (misses >= 4) {
                phase = MinigamePhase.LOST
                onGameEnd(score, bestCombo, false)
                break
            }

            val difficulty = (elapsedMs / 1_000f).coerceAtMost(16f)
            val maxSpots = if (elapsedMs >= DUAL_SPOT_AFTER_MS) 2 else 1

            val expired = activeSpots.filter { now >= it.expiresAtMs }
            if (expired.isNotEmpty()) {
                combo = 0
                misses += expired.size
                showFlash("TOO SLOW!")
                spawnCooldown = 18
            }
            activeSpots = activeSpots.filter { now < it.expiresAtMs }

            spawnCooldown -= 1
            if (spawnCooldown <= 0 && activeSpots.size < maxSpots) {
                val occupied = activeSpots.map { it.cell }.toMutableSet()
                var spots = activeSpots
                while (spots.size < maxSpots) {
                    val newSpot = spawnSpot(now, difficulty, occupied) ?: break
                    spots = spots + newSpot
                    occupied.add(newSpot.cell)
                }
                activeSpots = spots
                spawnCooldown = max(10, 20 - combo)
            }
        }
    }

    val effectiveDaily = dailyTargetScore?.let { MinigameBalance.effectiveDailyTarget(gameId, it) }
    val subtitle = when (phase) {
        MinigamePhase.READY -> buildString {
            append("Tap mess spots before they fade — stay tidy!")
            effectiveDaily?.let { append(" · goal $it cleans") }
            ghostChallengeScore?.let { append(" · beat $it!") }
        }
        MinigamePhase.PLAYING -> "Fast taps build combos"
        MinigamePhase.WON -> "Sparkling clean! nibbli purrs."
        MinigamePhase.LOST -> "Keep swiping — you'll get faster!"
    }

    val shakeOffset = if (missShakeTicks > 0) {
        (if (missShakeTicks % 2 == 0) 4 else -4).dp
    } else {
        0.dp
    }

    RetroMinigameScaffold(
        title = "TIDY TAP",
        subtitle = subtitle,
        palette = palette,
        hud = if (phase != MinigamePhase.READY) {
            RetroHudState(
                scoreLabel = scoreGoalText(gameId, score, dailyTargetScore, ghostChallengeScore),
                livesLabel = "${4 - misses} left",
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
                    .offset(x = shakeOffset)
                    .pointerInput(phase, activeSpots) {
                        if (phase != MinigamePhase.PLAYING) return@pointerInput
                        detectTapGestures { offset ->
                            val cellW = size.width / GRID
                            val cellH = size.height / GRID
                            val col = (offset.x / cellW).toInt().coerceIn(0, GRID - 1)
                            val row = (offset.y / cellH).toInt().coerceIn(0, GRID - 1)
                            tapCell(row * GRID + col)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (phase == MinigamePhase.READY) {
                    RetroReadyPanel(text = "▶ TAP THE MESS", palette = palette)
                } else {
                    val now = System.currentTimeMillis()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val px = fieldPixelUnit()
                        val cellW = size.width / GRID
                        val cellH = size.height / GRID
                        drawRetroGrid(palette, cellW)
                        val pad = px * 0.75f

                        for (i in 0 until GRID * GRID) {
                            val col = i % GRID
                            val row = i / GRID
                            val left = col * cellW + pad
                            val top = row * cellH + pad
                            val holeW = cellW - pad * 2
                            val holeH = cellH - pad * 2
                            drawRoundRect(
                                color = palette.lcdDark.copy(alpha = 0.35f),
                                topLeft = Offset(left, top),
                                size = Size(holeW, holeH),
                                cornerRadius = CornerRadius(px * 0.35f, px * 0.35f),
                            )
                        }

                        activeSpots.forEach { spot ->
                            val col = spot.cell % GRID
                            val row = spot.cell / GRID
                            val left = col * cellW + pad
                            val top = row * cellH + pad
                            val holeW = cellW - pad * 2
                            val holeH = cellH - pad * 2
                            val cx = left + holeW / 2f
                            val cy = top + holeH / 2f
                            val totalLife = (spot.expiresAtMs - spot.spawnAtMs).coerceAtLeast(1L)
                            val remaining = (spot.expiresAtMs - now).coerceAtLeast(0L)
                            val lifeFrac = remaining.toFloat() / totalLife
                            val pulse = 0.85f + sin(elapsedMs / 120f) * 0.15f
                            val radius = minOf(holeW, holeH) * 0.2f * pulse

                            drawPixelCircle(
                                palette.danger.copy(alpha = 0.25f + lifeFrac * 0.35f),
                                cx,
                                cy,
                                radius * 1.6f,
                            )
                            drawPixelCircle(palette.danger, cx, cy, radius)
                            drawPixelRect(palette.pixel, cx - px * 0.45f, cy - px * 1.2f, px * 0.9f, px * 0.55f)

                            val ringRadius = radius * (1.2f + (1f - lifeFrac) * 0.5f)
                            drawCircle(
                                color = palette.highlight.copy(alpha = lifeFrac * 0.7f),
                                radius = ringRadius,
                                center = Offset(cx, cy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = px * 0.2f),
                            )
                        }
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
                        text = if (combo >= 3) "COMBO x$combo" else "FIND & TAP",
                        palette = palette,
                        accent = true,
                    )
                }
                MinigamePhase.WON -> {
                    RetroFooterText(text = "FINAL $score · COMBO x$bestCombo", palette = palette)
                    RetroPixelButton(text = "Claim reward", onClick = onWin, modifier = Modifier.fillMaxWidth(), palette = palette)
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
