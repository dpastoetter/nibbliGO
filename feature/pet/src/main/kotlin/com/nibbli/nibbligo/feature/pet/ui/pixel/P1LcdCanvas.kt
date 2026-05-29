package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.R

@Composable
fun P1LcdCanvas(
    pet: PetState,
    menuLabel: String,
    frameIndex: Int,
    modifier: Modifier = Modifier,
    flash: Boolean = false,
) {
    val atlas = ImageBitmap.imageResource(R.drawable.nibbli_sprites)
    val selection = pet.resolveSprite()
    val frame = if (frameIndex % 2 == 1 && selection.alternate != null) {
        selection.alternate
    } else {
        selection.primary
    }

    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "p1_bob")
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val bobOffset = if (selection.alternate != null || selection.primary == NibbliSpriteAtlas.Frame.PLAYFUL) {
        bob * 1.5f
    } else {
        0f
    }

    val colors = p1Colors()
    val lcdBg = if (flash) colors.lcdGreenDark else colors.lcdGreen

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(P1DisplaySpec.LCD_WIDTH_PX / P1DisplaySpec.LCD_HEIGHT_PX.toFloat()),
    ) {
        val scale = size.width / P1DisplaySpec.LCD_WIDTH_PX

        drawRect(color = lcdBg, size = size)
        drawRect(
            color = colors.lcdGreenDark,
            topLeft = Offset(scale, scale),
            size = Size(size.width - 2 * scale, size.height - 2 * scale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = scale),
        )

        val petZoneHeight = if (frame == NibbliSpriteAtlas.Frame.EGG) {
            P1DisplaySpec.PET_ZONE_HEIGHT_PX * 0.65f
        } else {
            P1DisplaySpec.PET_ZONE_HEIGHT_PX.toFloat()
        }
        val zoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX + bobOffset
        val zoneWidthPx = P1DisplaySpec.LCD_WIDTH_PX.toFloat()
        drawAtlasFrameInZone(
            atlas = atlas,
            frame = frame,
            zoneLeftPx = 0f,
            zoneTopPx = zoneTopPx,
            zoneWidthPx = zoneWidthPx,
            zoneHeightPx = petZoneHeight,
            lcdScale = scale,
        )
        if (pet.showsCosmeticOverlay()) {
            val cosmetic = pet.equippedCosmetic!!
            val overlayAlpha = if (cosmetic == PetCosmetic.AURORA_AURA && frameIndex % 2 == 1) {
                0.72f
            } else {
                1f
            }
            drawCosmeticOverlayInZone(
                atlas = atlas,
                overlay = cosmetic.toOverlay(),
                zoneLeftPx = 0f,
                zoneTopPx = zoneTopPx,
                zoneWidthPx = zoneWidthPx,
                zoneHeightPx = petZoneHeight,
                lcdScale = scale,
                alpha = overlayAlpha,
            )
        }

        drawMenuLabel(
            label = menuLabel,
            topPx = P1DisplaySpec.MENU_BAND_TOP_PX.toFloat(),
            lcdScale = scale,
        )

        drawStatIconStrip(pet = pet, colors = colors, lcdScale = scale)
    }
}

private fun DrawScope.drawMenuLabel(label: String, topPx: Float, lcdScale: Float) {
    val text = label.uppercase().take(8)
    val textSizePx = 6f * lcdScale
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A1A1E")
        this.textSize = textSizePx
        typeface = Typeface.MONOSPACE
        isAntiAlias = false
    }
    val textWidth = paint.measureText(text)
    val x = (size.width - textWidth) / 2f
    val y = (topPx + 6f) * lcdScale
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(text, x, y, paint)
    }
}

private fun DrawScope.drawStatIconStrip(pet: PetState, colors: P1Colors, lcdScale: Float) {
    val icons = buildList {
        add(P1StatIcon.HUNGER to (pet.stats.hunger < 45))
        add(P1StatIcon.HAPPY to (pet.stats.happiness < 45))
        add(P1StatIcon.ENERGY to (pet.stats.energy < 30))
        if (pet.hasMess) add(P1StatIcon.MESS to true)
        if (pet.condition == PetCondition.SICK || pet.stats.health < 35) {
            add(P1StatIcon.SICK to true)
        }
    }.take(4)

    val iconPx = P1DisplaySpec.ICON_SIZE_PX * P1DisplaySpec.ICON_SCALE
    val totalWidth = icons.size * iconPx + (icons.size - 1).coerceAtLeast(0) * 4
    var x = (P1DisplaySpec.LCD_WIDTH_PX - totalWidth) / 2f
    val y = P1DisplaySpec.ICON_STRIP_TOP_PX.toFloat()

    icons.forEach { (icon, alert) ->
        drawP1Icon(
            icon = icon,
            topLeft = Offset(x * lcdScale, y * lcdScale),
            scale = P1DisplaySpec.ICON_SCALE * lcdScale,
            color = if (alert) colors.lcdPixel else colors.lcdPixel.copy(alpha = 0.3f),
        )
        x += iconPx + 4
    }
}

data class SpriteDstRect(
    val left: Float,
    val top: Float,
    val spriteW: Float,
    val spriteH: Float,
)

fun computeSpriteDstRect(
    zoneLeftPx: Float,
    zoneTopPx: Float,
    zoneWidthPx: Float,
    zoneHeightPx: Float,
): SpriteDstRect {
    val framePx = NibbliSpriteAtlas.FRAME_SIZE_PX
    val fitScale = minOf(zoneWidthPx / framePx, zoneHeightPx / framePx)
    val spriteW = framePx * fitScale
    val spriteH = framePx * fitScale
    val left = zoneLeftPx + (zoneWidthPx - spriteW) / 2f
    val top = zoneTopPx + (zoneHeightPx - spriteH) / 2f
    return SpriteDstRect(left = left, top = top, spriteW = spriteW, spriteH = spriteH)
}

fun DrawScope.drawAtlasFrameInZone(
    atlas: ImageBitmap,
    frame: NibbliSpriteAtlas.Frame,
    zoneLeftPx: Float,
    zoneTopPx: Float,
    zoneWidthPx: Float,
    zoneHeightPx: Float,
    lcdScale: Float,
) {
    val rect = computeSpriteDstRect(zoneLeftPx, zoneTopPx, zoneWidthPx, zoneHeightPx)
    val framePx = NibbliSpriteAtlas.FRAME_SIZE_PX
    drawImage(
        image = atlas,
        srcOffset = IntOffset(frame.col * framePx, 0),
        srcSize = IntSize(framePx, framePx),
        dstOffset = IntOffset((rect.left * lcdScale).toInt(), (rect.top * lcdScale).toInt()),
        dstSize = IntSize((rect.spriteW * lcdScale).toInt(), (rect.spriteH * lcdScale).toInt()),
        filterQuality = FilterQuality.None,
    )
}

fun DrawScope.drawCosmeticOverlayInZone(
    atlas: ImageBitmap,
    overlay: NibbliSpriteAtlas.CosmeticOverlay,
    zoneLeftPx: Float,
    zoneTopPx: Float,
    zoneWidthPx: Float,
    zoneHeightPx: Float,
    lcdScale: Float,
    alpha: Float = 1f,
) {
    val rect = computeSpriteDstRect(zoneLeftPx, zoneTopPx, zoneWidthPx, zoneHeightPx)
    val framePx = NibbliSpriteAtlas.FRAME_SIZE_PX
    drawImage(
        image = atlas,
        srcOffset = IntOffset(overlay.col * framePx, NibbliSpriteAtlas.OVERLAY_ROW_PX),
        srcSize = IntSize(framePx, framePx),
        dstOffset = IntOffset((rect.left * lcdScale).toInt(), (rect.top * lcdScale).toInt()),
        dstSize = IntSize((rect.spriteW * lcdScale).toInt(), (rect.spriteH * lcdScale).toInt()),
        alpha = alpha,
        filterQuality = FilterQuality.None,
    )
}
