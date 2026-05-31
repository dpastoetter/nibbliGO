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
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import kotlin.math.sin

fun DrawScope.drawLcdAmbientEffects(
    pet: PetState,
    frame: NibbliSpriteAtlas.Frame,
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    lcdScaleY: Float = lcdScaleX,
    colors: P1Colors,
    frameIndex: Int,
) {
    if (pet.condition == PetCondition.SLEEPING || frame == NibbliSpriteAtlas.Frame.SLEEPING) {
        drawSleepZzz(
            zoneTopPx = zoneTopPx,
            zoneHeightPx = zoneHeightPx,
            lcdScaleX = lcdScaleX,
            frameIndex = frameIndex,
        )
    }
    when (pet.animation) {
        PetAnimation.EAT -> drawEatCrumbs(
            zoneTopPx = zoneTopPx,
            zoneHeightPx = zoneHeightPx,
            lcdScaleX = lcdScaleX,
            colors = colors,
            frameIndex = frameIndex,
        )
        PetAnimation.PLAY -> {
            if (frameIndex % 3 == 0) {
                drawPlayfulHeart(
                    zoneTopPx = zoneTopPx,
                    zoneHeightPx = zoneHeightPx,
                    lcdScaleX = lcdScaleX,
                    colors = colors,
                    frameIndex = frameIndex,
                    offsetXFactor = 0.78f,
                )
            }
            if (frameIndex % 4 == 1) {
                drawSparkle(
                    zoneTopPx = zoneTopPx,
                    zoneHeightPx = zoneHeightPx,
                    lcdScaleX = lcdScaleX,
                    colors = colors,
                    frameIndex = frameIndex + 2,
                    offsetXFactor = 0.22f,
                )
            }
        }
        PetAnimation.HAPPY, PetAnimation.EVOLVE -> {
            if (frameIndex % 4 == 0) {
                drawPlayfulHeart(
                    zoneTopPx = zoneTopPx,
                    zoneHeightPx = zoneHeightPx,
                    lcdScaleX = lcdScaleX,
                    colors = colors,
                    frameIndex = frameIndex,
                    offsetXFactor = 0.76f,
                )
            }
            if (frameIndex % 5 == 2) {
                drawSparkle(
                    zoneTopPx = zoneTopPx,
                    zoneHeightPx = zoneHeightPx,
                    lcdScaleX = lcdScaleX,
                    colors = colors,
                    frameIndex = frameIndex,
                    offsetXFactor = 0.24f,
                )
            }
        }
        PetAnimation.ATTENTION -> if (frameIndex % 2 == 0) {
            drawAttentionPing(
                zoneTopPx = zoneTopPx,
                zoneHeightPx = zoneHeightPx,
                lcdScaleX = lcdScaleX,
                colors = colors,
                frameIndex = frameIndex,
            )
        }
        PetAnimation.IDLE -> if (pet.stats.mood >= 75 && frameIndex % 6 == 0) {
            drawPlayfulHeart(
                zoneTopPx = zoneTopPx,
                zoneHeightPx = zoneHeightPx,
                lcdScaleX = lcdScaleX,
                colors = colors,
                frameIndex = frameIndex,
                offsetXFactor = 0.78f,
            )
        }
        else -> Unit
    }
    if (pet.activeNeed != PetNeed.NONE && pet.animation != PetAnimation.ATTENTION) {
        if (frameIndex % 4 == 1) {
            drawAttentionPing(
                zoneTopPx = zoneTopPx,
                zoneHeightPx = zoneHeightPx,
                lcdScaleX = lcdScaleX,
                colors = colors,
                frameIndex = frameIndex,
            )
        }
    }
    if (
        frame == NibbliSpriteAtlas.Frame.HUNGRY &&
        pet.animation != PetAnimation.EAT
    ) {
        if (frameIndex % 3 == 0) {
            drawHungryDots(
                zoneTopPx = zoneTopPx,
                zoneHeightPx = zoneHeightPx,
                lcdScaleX = lcdScaleX,
                colors = colors,
                frameIndex = frameIndex,
            )
        }
    }
}

private fun DrawScope.drawSleepZzz(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    frameIndex: Int,
) {
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A1A1E")
        textSize = 5f * lcdScaleX
        typeface = Typeface.MONOSPACE
        isAntiAlias = false
    }
    val baseX = P1DisplaySpec.LCD_WIDTH_PX * 0.72f
    val baseY = zoneTopPx + zoneHeightPx * 0.15f
    repeat(3) { i ->
        val phase = (frameIndex + i * 5) % 16
        val driftY = phase * 0.45f
        val driftX = sin((frameIndex + i * 3) * 0.45f) * 1.6f
        val alpha = (0.35f + (phase / 16f) * 0.55f).coerceIn(0.35f, 0.9f)
        paint.alpha = (alpha * 255).toInt()
        val zText = "z".repeat(i + 1)
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                zText,
                (baseX + driftX + i * 3f) * lcdScaleX,
                (baseY - driftY + i * 2f) * lcdScaleX,
                paint,
            )
        }
    }
}

private fun DrawScope.drawEatCrumbs(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    colors: P1Colors,
    frameIndex: Int,
) {
    val pixel = lcdScaleX
    val color = colors.lcdPixel.copy(alpha = 0.7f)
    val baseX = P1DisplaySpec.LCD_WIDTH_PX * 0.62f
    val baseY = zoneTopPx + zoneHeightPx * 0.72f
    repeat(3) { i ->
        val phase = (frameIndex + i * 2) % 8
        val drop = phase * 0.6f
        val spread = sin((frameIndex + i) * 0.9f) * 2f + i * 2.5f
        drawRect(
            color = color.copy(alpha = 0.5f + (1f - phase / 8f) * 0.4f),
            topLeft = Offset(
                (baseX + spread) * lcdScaleX - pixel / 2f,
                (baseY + drop) * lcdScaleX - pixel / 2f,
            ),
            size = Size(pixel, pixel),
        )
    }
}

private fun DrawScope.drawHungryDots(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    colors: P1Colors,
    frameIndex: Int,
) {
    val pixel = lcdScaleX
    val color = colors.lcdPixel.copy(alpha = 0.65f)
    val baseX = P1DisplaySpec.LCD_WIDTH_PX * 0.68f
    val baseY = zoneTopPx + zoneHeightPx * 0.18f
    repeat(3) { i ->
        val bounce = sin((frameIndex + i * 2) * 0.8f) * 1.2f
        drawRect(
            color = color,
            topLeft = Offset(
                (baseX + i * 3f) * lcdScaleX - pixel / 2f,
                (baseY + bounce - i * 0.5f) * lcdScaleX - pixel / 2f,
            ),
            size = Size(pixel, pixel),
        )
    }
}

private fun DrawScope.drawAttentionPing(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    colors: P1Colors,
    frameIndex: Int,
) {
    val pixel = lcdScaleX
    val pulse = 0.55f + sin(frameIndex * 1.1f) * 0.35f
    val color = colors.lcdPixel.copy(alpha = pulse)
    val centerX = P1DisplaySpec.LCD_WIDTH_PX * 0.82f
    val centerY = zoneTopPx + zoneHeightPx * 0.14f
    listOf(
        Offset(0f, -2f),
        Offset(-1f, -1f), Offset(1f, -1f),
        Offset(-2f, 0f), Offset(0f, 0f), Offset(2f, 0f),
        Offset(-1f, 1f), Offset(1f, 1f),
    ).forEach { offset ->
        drawRect(
            color = color,
            topLeft = Offset(
                (centerX + offset.x) * lcdScaleX - pixel / 2f,
                (centerY + offset.y) * lcdScaleX - pixel / 2f,
            ),
            size = Size(pixel, pixel),
        )
    }
}

private fun DrawScope.drawSparkle(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    colors: P1Colors,
    frameIndex: Int,
    offsetXFactor: Float,
) {
    val pixel = lcdScaleX
    val bob = sin(frameIndex * 0.9f) * 1.8f
    val centerX = P1DisplaySpec.LCD_WIDTH_PX * offsetXFactor
    val centerY = zoneTopPx + zoneHeightPx * 0.28f + bob
    val color = colors.lcdPixel.copy(alpha = 0.6f + sin(frameIndex * 0.6f) * 0.25f)
    listOf(
        Offset(0f, -2f), Offset(0f, 2f),
        Offset(-2f, 0f), Offset(2f, 0f),
        Offset(0f, 0f),
    ).forEach { offset ->
        drawRect(
            color = color,
            topLeft = Offset(
                (centerX + offset.x) * lcdScaleX - pixel / 2f,
                (centerY + offset.y) * lcdScaleX - pixel / 2f,
            ),
            size = Size(pixel, pixel),
        )
    }
}

private fun DrawScope.drawPlayfulHeart(
    zoneTopPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    colors: P1Colors,
    frameIndex: Int,
    offsetXFactor: Float = 0.78f,
) {
    val bob = sin(frameIndex * 0.7f) * 2f
    val centerX = P1DisplaySpec.LCD_WIDTH_PX * offsetXFactor
    val centerY = zoneTopPx + zoneHeightPx * 0.22f + bob
    val pixel = lcdScaleX
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
                (centerX + offset.x) * lcdScaleX - pixel / 2f,
                (centerY + offset.y) * lcdScaleX - pixel / 2f,
            ),
            size = Size(pixel, pixel),
        )
    }
}
