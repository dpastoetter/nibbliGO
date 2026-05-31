package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.equippedScene
import com.nibbli.nibbligo.core.model.PetState
import kotlin.math.sin

fun DrawScope.drawLcdScene(
    pet: PetState,
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    frameIndex: Int,
    zoneTopPx: Float,
    zoneHeightPx: Float,
) {
    when (pet.equippedScene) {
        PetLcdScene.COZY -> drawCozyScene(lcdScaleX, colors)
        PetLcdScene.STARS -> drawStarsScene(lcdScaleX, lcdScaleY, colors, frameIndex, zoneTopPx, zoneHeightPx)
        PetLcdScene.CLOUDS -> drawCloudsScene(lcdScaleX, lcdScaleY, colors, zoneTopPx)
        PetLcdScene.NIGHT -> drawNightScene(lcdScaleX, lcdScaleY, colors, frameIndex, zoneTopPx)
    }
}

private fun DrawScope.drawCozyScene(lcdScaleX: Float, colors: P1Colors) {
    val px = lcdScaleX
    val dot = colors.lcdPixel.copy(alpha = 0.12f)
    var x = px * 4f
    while (x < size.width) {
        var y = px * 4f
        while (y < size.height - px * 10f) {
            drawRect(color = dot, topLeft = Offset(x, y), size = Size(px * 0.5f, px * 0.5f))
            y += px * 6f
        }
        x += px * 6f
    }
}

private fun DrawScope.drawStarsScene(
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    frameIndex: Int,
    zoneTopPx: Float,
    zoneHeightPx: Float,
) {
    drawCozyScene(lcdScaleX, colors)
    val px = lcdScaleX
    val starColor = colors.lcdPixel.copy(alpha = 0.55f)
    val seeds = listOf(0.12f, 0.28f, 0.44f, 0.62f, 0.78f, 0.9f)
    seeds.forEachIndexed { i, xf ->
        val twinkle = 0.35f + sin((frameIndex + i * 3) * 0.55f) * 0.35f
        if (twinkle < 0.25f) return@forEachIndexed
        val x = P1DisplaySpec.LCD_WIDTH_PX * xf
        val y = zoneTopPx + zoneHeightPx * (0.15f + (i % 3) * 0.18f)
        drawRect(
            color = starColor.copy(alpha = twinkle),
            topLeft = Offset(x * lcdScaleX - px / 2f, y * lcdScaleY - px / 2f),
            size = Size(px, px),
        )
    }
}

private fun DrawScope.drawCloudsScene(
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    zoneTopPx: Float,
) {
    val px = lcdScaleX
    val band = colors.lcdPixel.copy(alpha = 0.14f)
    listOf(0.18f, 0.42f, 0.66f).forEach { yFactor ->
        val y = (zoneTopPx + P1DisplaySpec.PET_ZONE_HEIGHT_PX * yFactor) * lcdScaleY
        drawRect(color = band, topLeft = Offset(px * 2f, y), size = Size(size.width - px * 4f, px * 1.2f))
    }
}

private fun DrawScope.drawNightScene(
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    frameIndex: Int,
    zoneTopPx: Float,
) {
    drawRect(color = colors.lcdPixel.copy(alpha = 0.25f), size = size)
    drawStarsScene(lcdScaleX, lcdScaleY, colors, frameIndex, zoneTopPx, P1DisplaySpec.PET_ZONE_HEIGHT_PX.toFloat())
    val px = lcdScaleX
    val moonX = P1DisplaySpec.LCD_WIDTH_PX * 0.82f
    val moonY = zoneTopPx + 6f
    drawRect(
        color = colors.lcdPixel.copy(alpha = 0.85f),
        topLeft = Offset(moonX * lcdScaleX - px * 1.5f, moonY * lcdScaleY - px * 1.5f),
        size = Size(px * 3f, px * 3f),
    )
}
