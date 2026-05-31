package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetState
import kotlin.math.sin

fun petPropVerticalOffset(prop: PetLcdProp?): Float = when (prop) {
    PetLcdProp.BLANKET -> 2f
    else -> 0f
}

fun DrawScope.drawLcdProp(
    pet: PetState,
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    frameIndex: Int,
    zoneTopPx: Float,
    zoneHeightPx: Float,
    zoneWidthPx: Float,
) {
    when (pet.equippedProp) {
        PetLcdProp.BALL -> drawBallProp(lcdScaleX, lcdScaleY, colors, frameIndex, zoneTopPx, zoneHeightPx, zoneWidthPx)
        PetLcdProp.PLANT -> drawPlantProp(lcdScaleX, lcdScaleY, colors, zoneTopPx, zoneHeightPx, zoneWidthPx)
        PetLcdProp.BLANKET -> drawBlanketProp(lcdScaleX, lcdScaleY, colors, zoneTopPx, zoneHeightPx, zoneWidthPx)
        null -> Unit
    }
}

private fun DrawScope.drawBallProp(
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    frameIndex: Int,
    zoneTopPx: Float,
    zoneHeightPx: Float,
    zoneWidthPx: Float,
) {
    val px = lcdScaleX
    val bounce = sin(frameIndex * 0.85f) * 1.5f
    val cx = zoneWidthPx * 0.78f
    val cy = zoneTopPx + zoneHeightPx * 0.78f - bounce
    drawRect(
        color = colors.lcdPixel,
        topLeft = Offset(cx * lcdScaleX - px * 1.2f, cy * lcdScaleY - px * 1.2f),
        size = Size(px * 2.4f, px * 2.4f),
    )
    drawRect(
        color = colors.lcdGreen.copy(alpha = 0.5f),
        topLeft = Offset(cx * lcdScaleX - px * 0.4f, cy * lcdScaleY - px * 0.8f),
        size = Size(px * 0.6f, px * 0.6f),
    )
}

private fun DrawScope.drawPlantProp(
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    zoneTopPx: Float,
    zoneHeightPx: Float,
    zoneWidthPx: Float,
) {
    val px = lcdScaleX
    val baseX = zoneWidthPx * 0.1f
    val baseY = zoneTopPx + zoneHeightPx * 0.72f
    drawRect(
        color = colors.lcdGreenDark.copy(alpha = 0.7f),
        topLeft = Offset(baseX * lcdScaleX, baseY * lcdScaleY),
        size = Size(px * 2.2f, px * 2f),
    )
    drawRect(
        color = colors.lcdPixel,
        topLeft = Offset((baseX + 0.4f) * lcdScaleX, (baseY - 2.5f) * lcdScaleY),
        size = Size(px * 1.4f, px * 1.4f),
    )
    drawRect(
        color = colors.lcdPixel.copy(alpha = 0.75f),
        topLeft = Offset((baseX + 1.2f) * lcdScaleX, (baseY - 3.5f) * lcdScaleY),
        size = Size(px, px),
    )
}

private fun DrawScope.drawBlanketProp(
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    zoneTopPx: Float,
    zoneHeightPx: Float,
    zoneWidthPx: Float,
) {
    val px = lcdScaleX
    val left = zoneWidthPx * 0.28f
    val top = zoneTopPx + zoneHeightPx * 0.82f
    val width = zoneWidthPx * 0.44f
    drawRect(
        color = colors.lcdGreenDark.copy(alpha = 0.28f),
        topLeft = Offset(left * lcdScaleX, top * lcdScaleY),
        size = Size(width * lcdScaleX, px * 1.4f),
    )
    drawRect(
        color = colors.lcdPixel.copy(alpha = 0.35f),
        topLeft = Offset((left + 2f) * lcdScaleX, (top + 0.3f) * lcdScaleY),
        size = Size((width - 4f) * lcdScaleX, px * 0.5f),
    )
}
