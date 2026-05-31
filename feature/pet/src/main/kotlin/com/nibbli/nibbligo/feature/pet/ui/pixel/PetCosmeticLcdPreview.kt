package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.feature.pet.R

@Composable
fun PetCosmeticLcdPreview(
    equippedCosmetic: PetCosmetic?,
    modifier: Modifier = Modifier,
) {
    val atlas = ImageBitmap.imageResource(R.drawable.nibbli_sprites)
    val colors = p1Colors()
    val lcdWidth = P1DisplaySpec.LCD_WIDTH_PX.toFloat()
    val petZoneHeight = P1DisplaySpec.PET_ZONE_HEIGHT_PX.toFloat()

    Canvas(
        modifier = modifier
            .width(96.dp)
            .aspectRatio(P1DisplaySpec.lcdAspectRatio),
    ) {
        val lcdScaleX = size.width / lcdWidth
        val lcdScaleY = size.height / P1DisplaySpec.LCD_HEIGHT_PX
        drawRect(color = colors.lcdGreen, size = size)
        drawAtlasFrameInZone(
            atlas = atlas,
            frame = NibbliSpriteAtlas.Frame.IDLE_A,
            zoneLeftPx = 0f,
            zoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX.toFloat(),
            zoneWidthPx = lcdWidth,
            zoneHeightPx = petZoneHeight,
            lcdScaleX = lcdScaleX,
            lcdScaleY = lcdScaleY,
        )
        equippedCosmetic?.let { cosmetic ->
            drawCosmeticOverlayInZone(
                atlas = atlas,
                overlay = cosmetic.toOverlay(),
                zoneLeftPx = 0f,
                zoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX.toFloat(),
                zoneWidthPx = lcdWidth,
                zoneHeightPx = petZoneHeight,
                lcdScaleX = lcdScaleX,
                lcdScaleY = lcdScaleY,
            )
        }
        drawRect(
            color = colors.lcdGreenDark,
            topLeft = Offset(lcdScaleX, lcdScaleY),
            size = Size(size.width - 2 * lcdScaleX, size.height - 2 * lcdScaleY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = lcdScaleX),
        )
    }
}
