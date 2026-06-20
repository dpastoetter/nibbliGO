package com.nibbli.nibbligo.feature.pet.ui.pixel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nibbli.nibbligo.feature.pet.R
import com.nibbli.nibbligo.feature.pet.ui.collection.LcdCollectionItemRef
import com.nibbli.nibbligo.feature.pet.ui.collection.lcdItemDisplayName

@Composable
fun PetLcdItemPreview(
    itemRef: LcdCollectionItemRef,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    contentDescription: String? = null,
) {
    val atlas = ImageBitmap.imageResource(R.drawable.nibbli_sprites)
    val colors = p1Colors()
    val lcdWidth = P1DisplaySpec.LCD_WIDTH_PX.toFloat()
    val petZoneHeight = P1DisplaySpec.PET_ZONE_HEIGHT_PX.toFloat()
    val zoneTopPx = P1DisplaySpec.PET_ZONE_TOP_PX.toFloat()
    val description = contentDescription ?: buildPreviewDescription(itemRef, locked)

    Canvas(
        modifier = modifier
            .size(width = 44.dp, height = 36.dp)
            .aspectRatio(P1DisplaySpec.lcdAspectRatio)
            .alpha(if (locked) 0.45f else 1f)
            .semantics { this.contentDescription = description },
    ) {
        val lcdScaleX = size.width / lcdWidth
        val lcdScaleY = size.height / P1DisplaySpec.LCD_HEIGHT_PX

        drawRect(color = colors.lcdGreen, size = size)

        when (itemRef) {
            is LcdCollectionItemRef.Scene -> {
                drawLcdScenePreview(
                    scene = itemRef.scene,
                    lcdScaleX = lcdScaleX,
                    lcdScaleY = lcdScaleY,
                    colors = colors,
                    frameIndex = 0,
                    zoneTopPx = zoneTopPx,
                    zoneHeightPx = petZoneHeight,
                )
            }
            is LcdCollectionItemRef.Prop -> {
                drawLcdPropPreview(
                    prop = itemRef.prop,
                    lcdScaleX = lcdScaleX,
                    lcdScaleY = lcdScaleY,
                    colors = colors,
                    frameIndex = 0,
                    zoneTopPx = zoneTopPx,
                    zoneHeightPx = petZoneHeight,
                    zoneWidthPx = lcdWidth,
                )
            }
            is LcdCollectionItemRef.Wearable -> Unit
        }

        drawAtlasFrameInZone(
            atlas = atlas,
            frame = NibbliSpriteAtlas.Frame.IDLE_A,
            zoneLeftPx = 0f,
            zoneTopPx = zoneTopPx,
            zoneWidthPx = lcdWidth,
            zoneHeightPx = petZoneHeight,
            lcdScaleX = lcdScaleX,
            lcdScaleY = lcdScaleY,
        )

        if (itemRef is LcdCollectionItemRef.Wearable) {
            drawCosmeticOverlayInZone(
                atlas = atlas,
                overlay = itemRef.cosmetic.toOverlay(),
                zoneLeftPx = 0f,
                zoneTopPx = zoneTopPx,
                zoneWidthPx = lcdWidth,
                zoneHeightPx = petZoneHeight,
                lcdScaleX = lcdScaleX,
                lcdScaleY = lcdScaleY,
            )
        }

        drawRect(
            color = colors.lcdPixel.copy(alpha = 0.35f),
            topLeft = Offset(lcdScaleX, lcdScaleY),
            size = Size(size.width - 2 * lcdScaleX, size.height - 2 * lcdScaleY),
            style = Stroke(width = lcdScaleX),
        )
    }
}

private fun buildPreviewDescription(itemRef: LcdCollectionItemRef, locked: Boolean): String {
    val name = when (itemRef) {
        is LcdCollectionItemRef.Wearable -> lcdItemDisplayName(itemRef.cosmetic)
        is LcdCollectionItemRef.Scene -> lcdItemDisplayName(itemRef.scene)
        is LcdCollectionItemRef.Prop -> lcdItemDisplayName(itemRef.prop)
    }
    return "$name, ${if (locked) "locked" else "unlocked"}"
}
