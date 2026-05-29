package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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

    Canvas(modifier = modifier.size(96.dp, 72.dp)) {
        val lcdScale = size.width / lcdWidth
        drawRect(color = colors.lcdGreen, size = size)
        drawAtlasFrameInZone(
            atlas = atlas,
            frame = NibbliSpriteAtlas.Frame.IDLE_A,
            zoneLeftPx = 0f,
            zoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX.toFloat(),
            zoneWidthPx = lcdWidth,
            zoneHeightPx = petZoneHeight,
            lcdScale = lcdScale,
        )
        equippedCosmetic?.let { cosmetic ->
            drawCosmeticOverlayInZone(
                atlas = atlas,
                overlay = cosmetic.toOverlay(),
                zoneLeftPx = 0f,
                zoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX.toFloat(),
                zoneWidthPx = lcdWidth,
                zoneHeightPx = petZoneHeight,
                lcdScale = lcdScale,
            )
        }
        drawRect(
            color = colors.lcdGreenDark,
            topLeft = Offset(lcdScale, lcdScale),
            size = Size(size.width - 2 * lcdScale, size.height - 2 * lcdScale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = lcdScale),
        )
    }
}
