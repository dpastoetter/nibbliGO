package com.nibbli.nibbligo.feature.pet.ui.minigame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.feature.pet.ui.pixel.P1Theme
import com.nibbli.nibbligo.feature.pet.ui.pixel.p1Colors

internal data class RetroPalette(
    val shellBody: Color,
    val shellBorder: Color,
    val lcdWell: Color,
    val lcdBg: Color,
    val lcdDark: Color,
    val pixel: Color,
    val accent: Color,
    val highlight: Color,
    val danger: Color,
)

@Composable
internal fun rememberRetroPalette(): RetroPalette {
    val p1 = p1Colors()
    return RetroPalette(
        shellBody = p1.shellBody,
        shellBorder = p1.shellBorder,
        lcdWell = p1.lcdWell,
        lcdBg = p1.lcdGreen,
        lcdDark = p1.lcdGreenDark,
        pixel = p1.lcdPixel,
        accent = Color(0xFF2E4A28),
        highlight = Color(0xFFE8D878),
        danger = Color(0xFF8B3030),
    )
}

@Composable
fun RetroArcadeShell(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = rememberRetroPalette()
    Box(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.88f)
            .heightIn(min = 420.dp)
            .clip(RoundedCornerShape(P1Theme.ShellRadius))
            .background(palette.shellBody)
            .border(1.dp, palette.shellBorder, RoundedCornerShape(P1Theme.ShellRadius))
            .padding(12.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "nibbli GO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
                color = palette.lcdDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(P1Theme.LcdWellRadius))
                    .background(palette.lcdWell)
                    .padding(P1Theme.LcdMargin),
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun RetroMinigameScaffold(
    title: String,
    subtitle: String,
    palette: RetroPalette,
    hud: RetroHudState? = null,
    field: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    onClose: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = palette.pixel,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = palette.lcdDark,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 320.dp)
                .background(palette.lcdDark, RoundedCornerShape(2.dp))
                .border(2.dp, palette.pixel, RoundedCornerShape(2.dp))
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.lcdBg, RoundedCornerShape(1.dp))
                    .border(1.dp, palette.lcdDark.copy(alpha = 0.45f), RoundedCornerShape(1.dp))
                    .padding(1.dp),
            ) {
                field()
                if (hud != null) {
                    RetroHudBar(hud = hud, palette = palette, modifier = Modifier.align(Alignment.TopCenter))
                }
                RetroScanlines(palette)
                RetroVignette(palette)
            }
        }
        footer()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onBack != null) {
                RetroNavButton(text = "← GAMES", onClick = onBack, palette = palette)
            } else {
                Box(modifier = Modifier)
            }
            RetroNavButton(text = "CLOSE", onClick = onClose, palette = palette)
        }
    }
}

internal data class RetroHudState(
    val scoreLabel: String,
    val livesLabel: String,
    val timeLabel: String,
)

@Composable
private fun RetroHudBar(
    hud: RetroHudState,
    palette: RetroPalette,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.lcdDark.copy(alpha = 0.92f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        RetroHudCell("SCR", hud.scoreLabel, palette)
        RetroHudCell("LIV", hud.livesLabel, palette)
        RetroHudCell("TIM", hud.timeLabel, palette)
    }
}

@Composable
private fun RetroHudCell(label: String, value: String, palette: RetroPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = palette.lcdBg.copy(alpha = 0.75f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = palette.lcdBg,
        )
    }
}

@Composable
private fun RetroScanlines(palette: RetroPalette) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 4f
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = palette.lcdDark.copy(alpha = 0.07f),
                topLeft = Offset(0f, y),
                size = Size(size.width, 1.5f),
            )
            y += step
        }
    }
}

@Composable
internal fun RetroFlashText(text: String, palette: RetroPalette, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        ),
        color = palette.accent,
    )
}

@Composable
internal fun RetroPixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    palette: RetroPalette = rememberRetroPalette(),
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(palette.lcdDark, RoundedCornerShape(2.dp))
            .border(2.dp, palette.pixel, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = palette.lcdBg,
        )
    }
}

@Composable
private fun RetroVignette(palette: RetroPalette) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val edge = palette.lcdDark.copy(alpha = 0.22f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, edge),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.72f,
            ),
        )
    }
}

@Composable
internal fun RetroReadyPanel(text: String, palette: RetroPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(2.dp, palette.lcdDark, RoundedCornerShape(0.dp))
            .background(palette.lcdBg.copy(alpha = 0.92f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = palette.lcdDark,
        )
    }
}

@Composable
internal fun RetroFooterText(
    text: String,
    palette: RetroPalette,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = if (accent) palette.accent else palette.lcdDark,
    )
}

@Composable
internal fun RetroNavButton(
    text: String,
    onClick: () -> Unit,
    palette: RetroPalette,
) {
    Box(
        modifier = Modifier
            .border(1.dp, palette.lcdDark, RoundedCornerShape(0.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            color = palette.lcdDark,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

internal fun DrawScope.fieldPixelUnit(): Float = minOf(size.width, size.height) / 32f

internal fun DrawScope.drawRetroGrid(palette: RetroPalette, cellPx: Float? = null) {
    val cell = cellPx ?: fieldPixelUnit()
    var x = 0f
    while (x < size.width) {
        drawLine(
            color = palette.lcdDark.copy(alpha = 0.18f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f,
        )
        x += cell
    }
    var y = 0f
    while (y < size.height) {
        drawLine(
            color = palette.lcdDark.copy(alpha = 0.18f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f,
        )
        y += cell
    }
}

internal fun DrawScope.drawPixelRect(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
) {
    drawRect(color = color, topLeft = Offset(left, top), size = Size(width, height))
}

internal fun DrawScope.drawPixelCircle(color: Color, cx: Float, cy: Float, radius: Float) {
    val r = radius.toInt().coerceAtLeast(1)
    for (dy in -r..r) {
        for (dx in -r..r) {
            if (dx * dx + dy * dy <= r * r) {
                drawRect(
                    color = color,
                    topLeft = Offset(cx + dx, cy + dy),
                    size = Size(1f, 1f),
                )
            }
        }
    }
}

internal enum class MinigamePhase {
    READY,
    PLAYING,
    WON,
    LOST,
}

internal fun scoreGoalText(
    gameId: PetMinigameId,
    score: Int,
    dailyTargetScore: Int?,
    ghostChallengeScore: Int?,
): String {
    val winScore = MinigameBalance.baseWinScore(gameId)
    val daily = dailyTargetScore?.let { MinigameBalance.effectiveDailyTarget(gameId, it) }
    return when {
        daily != null -> "$score/$daily"
        ghostChallengeScore != null -> "$score vs $ghostChallengeScore"
        else -> "$score/$winScore"
    }
}

internal fun wonByScore(
    gameId: PetMinigameId,
    score: Int,
    dailyTargetScore: Int?,
    ghostChallengeScore: Int?,
): Boolean {
    val winScore = MinigameBalance.baseWinScore(gameId)
    val daily = dailyTargetScore?.let { MinigameBalance.effectiveDailyTarget(gameId, it) }
    return when {
        ghostChallengeScore != null -> score > ghostChallengeScore
        daily != null -> score >= daily
        else -> score >= winScore
    }
}

internal fun formatTimeRemaining(durationMs: Long, elapsedMs: Long): String {
    val remaining = ((durationMs - elapsedMs.coerceAtMost(durationMs)) / 1000).toInt()
    return "${remaining}s"
}
