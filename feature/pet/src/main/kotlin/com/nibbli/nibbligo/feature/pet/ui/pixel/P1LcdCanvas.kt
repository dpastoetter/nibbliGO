package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
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
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.R

@Composable
fun P1LcdCanvas(
    pet: PetState,
    menuLabel: String,
    frameIndex: Int,
    modifier: Modifier = Modifier,
    flash: Boolean = false,
    tapBoost: Boolean = false,
    dialogueVisible: Boolean = false,
    talkLcdMode: Boolean = false,
    visitPet: PetState? = null,
) {
    val atlas = ImageBitmap.imageResource(R.drawable.nibbli_sprites)
    val dualVisit = visitPet != null && !talkLcdMode
    val selection = pet.resolveSprite()
    val frame = selection.frameAtIndex(frameIndex)
    val motion = rememberLcdPetMotion(
        selection = selection,
        pet = pet,
        frameIndex = frameIndex,
        tapBoost = tapBoost,
        motionKey = "local",
    )
    val visitFrameIndex = frameIndex + 3
    val visitSelection = visitPet?.resolveSprite()
    val visitFrame = visitSelection?.frameAtIndex(visitFrameIndex)
    val visitMotion = visitPet?.let { guest ->
        rememberLcdPetMotion(
            selection = visitSelection!!,
            pet = guest,
            frameIndex = visitFrameIndex,
            tapBoost = false,
            phaseOffset = 0.5f,
            motionKey = "visit",
        )
    }

    val colors = p1Colors()
    val lcdBg = if (flash) colors.lcdGreenDark else colors.lcdGreen
    val urgentNeed = pet.activeNeed != PetNeed.NONE
    val statPulseTransition = rememberInfiniteTransition(label = "lcd_stat_pulse")
    val statPulse by statPulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (urgentNeed) 400 else 700, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "stat_pulse",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(P1DisplaySpec.lcdAspectRatio),
    ) {
        val scaleX = size.width / P1DisplaySpec.LCD_WIDTH_PX
        val scaleY = size.height / P1DisplaySpec.LCD_HEIGHT_PX

        drawRect(color = lcdBg, size = size)
        drawRect(
            color = colors.lcdGreenDark,
            topLeft = Offset(scaleX, scaleY),
            size = Size(size.width - 2 * scaleX, size.height - 2 * scaleY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = scaleX),
        )

        val petZoneHeight = when {
            talkLcdMode -> P1DisplaySpec.BOTTOM_STRIP_SLOT_PX.toFloat()
            dialogueVisible -> (P1DisplaySpec.DIALOGUE_ZONE_TOP_PX - P1DisplaySpec.PET_ZONE_TOP_PX).toFloat()
            frame == NibbliSpriteAtlas.Frame.EGG && !dualVisit -> P1DisplaySpec.PET_ZONE_HEIGHT_PX * 0.65f
            else -> P1DisplaySpec.PET_ZONE_HEIGHT_PX.toFloat()
        }
        val propOffset = if (talkLcdMode || dualVisit) 0f else petPropVerticalOffset(pet.equippedProp)
        val sceneZoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX.toFloat()
        val fullZoneWidthPx = P1DisplaySpec.LCD_WIDTH_PX.toFloat()

        if (!talkLcdMode) {
            drawLcdScene(
                pet = pet,
                lcdScaleX = scaleX,
                lcdScaleY = scaleY,
                colors = colors,
                frameIndex = frameIndex,
                zoneTopPx = sceneZoneTopPx,
                zoneHeightPx = petZoneHeight,
            )
            if (!dualVisit) {
                drawLcdProp(
                    pet = pet,
                    lcdScaleX = scaleX,
                    lcdScaleY = scaleY,
                    colors = colors,
                    frameIndex = frameIndex,
                    zoneTopPx = sceneZoneTopPx,
                    zoneHeightPx = petZoneHeight,
                    zoneWidthPx = fullZoneWidthPx,
                )
            }
        }

        if (dualVisit) {
            val guest = visitPet!!
            val guestSelection = visitSelection!!
            val guestFrame = visitFrame!!
            val guestMotion = visitMotion!!
            val slotWidth = P1DisplaySpec.VISIT_SLOT_WIDTH_PX.toFloat()
            drawLcdPetSprite(
                atlas = atlas,
                pet = pet,
                frame = frame,
                frameIndex = frameIndex,
                motion = motion,
                zoneLeftPx = P1DisplaySpec.VISIT_LEFT_SLOT_X.toFloat(),
                zoneTopPx = sceneZoneTopPx + motion.bobOffsetPx + propOffset,
                zoneWidthPx = slotWidth,
                zoneHeightPx = petZoneHeight,
                lcdScaleX = scaleX,
                lcdScaleY = scaleY,
                colors = colors,
                drawAmbient = true,
            )
            drawLcdPetSprite(
                atlas = atlas,
                pet = guest,
                frame = guestFrame,
                frameIndex = visitFrameIndex,
                motion = guestMotion,
                zoneLeftPx = P1DisplaySpec.VISIT_RIGHT_SLOT_X.toFloat(),
                zoneTopPx = sceneZoneTopPx + guestMotion.bobOffsetPx,
                zoneWidthPx = slotWidth,
                zoneHeightPx = petZoneHeight,
                lcdScaleX = scaleX,
                lcdScaleY = scaleY,
                colors = colors,
                drawAmbient = false,
            )
        } else {
            val zoneTopPx = if (talkLcdMode) {
                P1DisplaySpec.BOTTOM_STRIP_TOP_PX.toFloat()
            } else {
                sceneZoneTopPx + motion.bobOffsetPx + propOffset
            }
            val zoneLeftPx = if (talkLcdMode) {
                P1DisplaySpec.TALK_PET_LEFT_PX.toFloat()
            } else {
                0f
            }
            val zoneWidthPx = if (talkLcdMode) {
                P1DisplaySpec.BOTTOM_STRIP_SLOT_PX.toFloat()
            } else {
                fullZoneWidthPx
            }
            drawLcdPetSprite(
                atlas = atlas,
                pet = pet,
                frame = frame,
                frameIndex = frameIndex,
                motion = motion,
                zoneLeftPx = zoneLeftPx,
                zoneTopPx = zoneTopPx,
                zoneWidthPx = zoneWidthPx,
                zoneHeightPx = petZoneHeight,
                lcdScaleX = scaleX,
                lcdScaleY = scaleY,
                colors = colors,
                drawAmbient = !talkLcdMode,
                talkLcdMode = talkLcdMode,
            )
        }

        if (!dialogueVisible && !talkLcdMode) {
            drawMenuLabel(
                label = menuLabel,
                topPx = P1DisplaySpec.MENU_BAND_TOP_PX.toFloat(),
                lcdScaleX = scaleX,
                lcdScaleY = scaleY,
            )
        }

        drawStatIconStrip(
            pet = pet,
            colors = colors,
            lcdScaleX = scaleX,
            lcdScaleY = scaleY,
            pulsePhase = statPulse,
            talkLcdMode = talkLcdMode,
        )
    }
}

private fun DrawScope.drawLcdPetSprite(
    atlas: ImageBitmap,
    pet: PetState,
    frame: NibbliSpriteAtlas.Frame,
    frameIndex: Int,
    motion: LcdPetMotion,
    zoneLeftPx: Float,
    zoneTopPx: Float,
    zoneWidthPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    lcdScaleY: Float,
    colors: P1Colors,
    drawAmbient: Boolean,
    talkLcdMode: Boolean = false,
) {
    val talkSpriteScale = if (talkLcdMode) 1f else motion.scale
    drawAtlasFrameInZone(
        atlas = atlas,
        frame = frame,
        zoneLeftPx = zoneLeftPx,
        zoneTopPx = zoneTopPx,
        zoneWidthPx = zoneWidthPx,
        zoneHeightPx = zoneHeightPx,
        lcdScaleX = lcdScaleX,
        lcdScaleY = lcdScaleY,
        swayOffsetPx = if (talkLcdMode) 0f else motion.swayOffsetPx,
        spriteScale = talkSpriteScale,
        spriteScaleY = if (talkLcdMode) 1f else motion.scaleY,
        align = if (talkLcdMode) SpriteZoneAlign.BottomStart else SpriteZoneAlign.Center,
    )
    if (drawAmbient) {
        drawLcdAmbientEffects(
            pet = pet,
            frame = frame,
            zoneTopPx = zoneTopPx,
            zoneHeightPx = zoneHeightPx,
            lcdScaleX = lcdScaleX,
            lcdScaleY = lcdScaleY,
            colors = colors,
            frameIndex = frameIndex,
        )
    }
    if (pet.showsCosmeticOverlay() && !talkLcdMode) {
        val cosmetic = pet.equippedCosmetic!!
        val overlayAlpha = when {
            cosmetic == PetCosmetic.AURORA_AURA && frameIndex % 2 == 1 -> 0.65f
            cosmetic == PetCosmetic.SPARKLE_COLLAR && frameIndex % 3 == 0 -> 0.85f
            else -> 1f
        }
        drawCosmeticOverlayInZone(
            atlas = atlas,
            overlay = cosmetic.toOverlay(),
            zoneLeftPx = zoneLeftPx,
            zoneTopPx = zoneTopPx,
            zoneWidthPx = zoneWidthPx,
            zoneHeightPx = zoneHeightPx,
            lcdScaleX = lcdScaleX,
            lcdScaleY = lcdScaleY,
            alpha = overlayAlpha,
            swayOffsetPx = motion.swayOffsetPx,
            spriteScale = motion.scale,
            spriteScaleY = motion.scaleY,
        )
    }
}

private fun DrawScope.drawMenuLabel(label: String, topPx: Float, lcdScaleX: Float, lcdScaleY: Float) {
    val text = label.uppercase().take(8)
    val textSizePx = 6f * lcdScaleX
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#1A1A1E")
        this.textSize = textSizePx
        typeface = Typeface.MONOSPACE
        isAntiAlias = false
    }
    val textWidth = paint.measureText(text)
    val x = (size.width - textWidth) / 2f
    val y = (topPx + 6f) * lcdScaleY
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(text, x, y, paint)
    }
}

private fun DrawScope.drawStatIconStrip(
    pet: PetState,
    colors: P1Colors,
    lcdScaleX: Float,
    lcdScaleY: Float,
    pulsePhase: Float,
    talkLcdMode: Boolean = false,
) {
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
    val totalWidth = icons.size * iconPx + (icons.size - 1).coerceAtLeast(0) * P1DisplaySpec.BOTTOM_STRIP_GAP_PX
    var x = if (talkLcdMode) {
        P1DisplaySpec.TALK_STAT_ICONS_START_PX.toFloat()
    } else {
        (P1DisplaySpec.LCD_WIDTH_PX - totalWidth) / 2f
    }
    val y = P1DisplaySpec.ICON_STRIP_TOP_PX.toFloat()

    icons.forEach { (icon, alert) ->
        val alpha = when {
            !alert -> 0.3f
            else -> 0.65f + pulsePhase * 0.35f
        }
        drawP1Icon(
            icon = icon,
            topLeft = Offset(x * lcdScaleX, y * lcdScaleY),
            scale = P1DisplaySpec.ICON_SCALE * lcdScaleX,
            color = colors.lcdPixel.copy(alpha = alpha),
        )
        x += iconPx + P1DisplaySpec.BOTTOM_STRIP_GAP_PX
    }
}

data class SpriteDstRect(
    val left: Float,
    val top: Float,
    val spriteW: Float,
    val spriteH: Float,
)

enum class SpriteZoneAlign {
    Center,
    BottomStart,
}

fun computeSpriteDstRect(
    zoneLeftPx: Float,
    zoneTopPx: Float,
    zoneWidthPx: Float,
    zoneHeightPx: Float,
    spriteScale: Float = 1f,
    spriteScaleY: Float = 1f,
    swayOffsetPx: Float = 0f,
    align: SpriteZoneAlign = SpriteZoneAlign.Center,
): SpriteDstRect {
    val framePx = NibbliSpriteAtlas.FRAME_SIZE_PX
    val fitScale = minOf(zoneWidthPx / framePx, zoneHeightPx / framePx)
    val spriteW = framePx * fitScale * spriteScale
    val spriteH = framePx * fitScale * spriteScale * spriteScaleY
    val left = when (align) {
        SpriteZoneAlign.Center -> zoneLeftPx + zoneWidthPx / 2f + swayOffsetPx - spriteW / 2f
        SpriteZoneAlign.BottomStart -> zoneLeftPx
    }
    val top = when (align) {
        SpriteZoneAlign.Center -> zoneTopPx + zoneHeightPx / 2f - spriteH / 2f
        SpriteZoneAlign.BottomStart -> zoneTopPx + zoneHeightPx - spriteH
    }
    return SpriteDstRect(left = left, top = top, spriteW = spriteW, spriteH = spriteH)
}

fun DrawScope.drawAtlasFrameInZone(
    atlas: ImageBitmap,
    frame: NibbliSpriteAtlas.Frame,
    zoneLeftPx: Float,
    zoneTopPx: Float,
    zoneWidthPx: Float,
    zoneHeightPx: Float,
    lcdScaleX: Float,
    lcdScaleY: Float = lcdScaleX,
    swayOffsetPx: Float = 0f,
    spriteScale: Float = 1f,
    spriteScaleY: Float = 1f,
    align: SpriteZoneAlign = SpriteZoneAlign.Center,
) {
    val rect = computeSpriteDstRect(
        zoneLeftPx = zoneLeftPx,
        zoneTopPx = zoneTopPx,
        zoneWidthPx = zoneWidthPx,
        zoneHeightPx = zoneHeightPx,
        spriteScale = spriteScale,
        spriteScaleY = spriteScaleY,
        swayOffsetPx = swayOffsetPx,
        align = align,
    )
    val framePx = NibbliSpriteAtlas.FRAME_SIZE_PX
    drawImage(
        image = atlas,
        srcOffset = IntOffset(frame.col * framePx, 0),
        srcSize = IntSize(framePx, framePx),
        dstOffset = IntOffset((rect.left * lcdScaleX).toInt(), (rect.top * lcdScaleY).toInt()),
        dstSize = IntSize((rect.spriteW * lcdScaleX).toInt(), (rect.spriteH * lcdScaleY).toInt()),
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
    lcdScaleX: Float,
    lcdScaleY: Float = lcdScaleX,
    alpha: Float = 1f,
    swayOffsetPx: Float = 0f,
    spriteScale: Float = 1f,
    spriteScaleY: Float = 1f,
) {
    val rect = computeSpriteDstRect(
        zoneLeftPx = zoneLeftPx,
        zoneTopPx = zoneTopPx,
        zoneWidthPx = zoneWidthPx,
        zoneHeightPx = zoneHeightPx,
        spriteScale = spriteScale,
        spriteScaleY = spriteScaleY,
        swayOffsetPx = swayOffsetPx,
    )
    val framePx = NibbliSpriteAtlas.FRAME_SIZE_PX
    drawImage(
        image = atlas,
        srcOffset = IntOffset(overlay.col * framePx, NibbliSpriteAtlas.OVERLAY_ROW_PX),
        srcSize = IntSize(framePx, framePx),
        dstOffset = IntOffset((rect.left * lcdScaleX).toInt(), (rect.top * lcdScaleY).toInt()),
        dstSize = IntSize((rect.spriteW * lcdScaleX).toInt(), (rect.spriteH * lcdScaleY).toInt()),
        alpha = alpha,
        filterQuality = FilterQuality.None,
    )
}
