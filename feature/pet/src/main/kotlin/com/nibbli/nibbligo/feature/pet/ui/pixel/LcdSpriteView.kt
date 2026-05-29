package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.R

@Composable
fun LcdSpriteAnimator(
    pet: PetState,
    modifier: Modifier = Modifier,
    frameIndex: Int = 0,
) {
    val atlas = ImageBitmap.imageResource(R.drawable.nibbli_sprites)
    val selection = pet.resolveSprite()
    val frame = selection.frameAtIndex(frameIndex)
    val motion = rememberLcdPetMotion(
        selection = selection,
        pet = pet,
        frameIndex = frameIndex,
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lcdScale = size.width / P1DisplaySpec.LCD_WIDTH_PX
            val zoneHeight = if (frame == NibbliSpriteAtlas.Frame.EGG) {
                P1DisplaySpec.PET_ZONE_HEIGHT_PX * 0.65f
            } else {
                P1DisplaySpec.PET_ZONE_HEIGHT_PX.toFloat()
            }
            drawAtlasFrameInZone(
                atlas = atlas,
                frame = frame,
                zoneLeftPx = 0f,
                zoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX + motion.bobOffsetPx,
                zoneWidthPx = P1DisplaySpec.LCD_WIDTH_PX.toFloat(),
                zoneHeightPx = zoneHeight,
                lcdScale = lcdScale,
                swayOffsetPx = motion.swayOffsetPx,
                spriteScale = motion.scale,
                spriteScaleY = motion.scaleY,
            )
        }
    }
}

@Composable
fun LcdSpriteView(
    frame: NibbliSpriteAtlas.Frame,
    modifier: Modifier = Modifier,
    bobOffsetPx: Float = 0f,
) {
    val atlas = ImageBitmap.imageResource(R.drawable.nibbli_sprites)
    Canvas(modifier = modifier) {
        val lcdScale = size.width / P1DisplaySpec.LCD_WIDTH_PX
        drawAtlasFrameInZone(
            atlas = atlas,
            frame = frame,
            zoneLeftPx = 0f,
            zoneTopPx = bobOffsetPx,
            zoneWidthPx = P1DisplaySpec.LCD_WIDTH_PX.toFloat(),
            zoneHeightPx = size.height / lcdScale,
            lcdScale = lcdScale,
        )
    }
}
