package com.nibbli.nibbligo.feature.pet.ui.pixel

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetState
import kotlin.math.sin

fun DrawScope.drawLcdAmbientEffects(
    pet: PetState,
    frame: NibbliSpriteAtlas.Frame,
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScale: Float,
    colors: P1Colors,
    frameIndex: Int,
) {
    if (pet.condition == PetCondition.SLEEPING || frame == NibbliSpriteAtlas.Frame.SLEEPING) {
        drawSleepZzz(
            zoneTopPx = zoneTopPx,
            zoneHeightPx = zoneHeightPx,
            lcdScale = lcdScale,
            frameIndex = frameIndex,
        )
    }
    if (pet.animation == PetAnimation.PLAY ||
        pet.animation == PetAnimation.HAPPY ||
        pet.stats.mood >= 75
    ) {
        if (frameIndex % 6 == 0) {
            drawPlayfulHeart(
                zoneTopPx = zoneTopPx,
                zoneHeightPx = zoneHeightPx,
                lcdScale = lcdScale,
                colors = colors,
                frameIndex = frameIndex,
            )
        }
    }
}

private fun DrawScope.drawSleepZzz(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScale: Float,
    frameIndex: Int,
) {
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A1A1E")
        textSize = 5f * lcdScale
        typeface = Typeface.MONOSPACE
        isAntiAlias = false
    }
    val baseX = P1DisplaySpec.LCD_WIDTH_PX * 0.72f
    val baseY = zoneTopPx + zoneHeightPx * 0.15f
    repeat(3) { i ->
        val phase = (frameIndex + i * 5) % 16
        val driftY = phase * 0.35f
        val driftX = sin((frameIndex + i * 3) * 0.45f) * 1.2f
        val alpha = (0.35f + (phase / 16f) * 0.55f).coerceIn(0.35f, 0.9f)
        paint.alpha = (alpha * 255).toInt()
        val zText = "z".repeat(i + 1)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                zText,
                (baseX + driftX + i * 3f) * lcdScale,
                (baseY - driftY + i * 2f) * lcdScale,
                paint,
            )
        }
    }
}

private fun DrawScope.drawPlayfulHeart(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScale: Float,
    colors: P1Colors,
    frameIndex: Int,
) {
    val bob = sin(frameIndex * 0.7f) * 1.5f
    val centerX = P1DisplaySpec.LCD_WIDTH_PX * 0.78f
    val centerY = zoneTopPx + zoneHeightPx * 0.22f + bob
    val pixel = lcdScale
    val color = colors.lcdPixel.copy(alpha = 0.75f + sin(frameIndex * 0.5f) * 0.2f)
    val heart = listOf(
        Offset(-2f, -1f), Offset(-1f, -2f), Offset(0f, -2f), Offset(1f, -2f), Offset(2f, -1f),
        Offset(-2f, 0f), Offset(-1f, 0f), Offset(0f, 0f), Offset(1f, 0f), Offset(2f, 0f),
        Offset(-1f, 1f), Offset(0f, 1f), Offset(1f, 1f),
        Offset(0f, 2f),
    )
    heart.forEach { offset ->
        drawRect(
            color = color,
            topLeft = Offset(
                (centerX + offset.x) * lcdScale - pixel / 2f,
                (centerY + offset.y) * lcdScale - pixel / 2f,
            ),
            size = Size(pixel, pixel),
        )
    }
}
