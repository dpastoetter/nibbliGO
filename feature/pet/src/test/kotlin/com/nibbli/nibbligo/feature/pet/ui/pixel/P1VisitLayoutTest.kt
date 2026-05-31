package com.nibbli.nibbligo.feature.pet.ui.pixel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class P1VisitLayoutTest {

    @Test
    fun visitSlots_fitWithinLcdWidth() {
        val leftEnd = P1DisplaySpec.VISIT_LEFT_SLOT_X + P1DisplaySpec.VISIT_SLOT_WIDTH_PX
        val rightEnd = P1DisplaySpec.VISIT_RIGHT_SLOT_X + P1DisplaySpec.VISIT_SLOT_WIDTH_PX
        assertTrue(leftEnd < P1DisplaySpec.VISIT_RIGHT_SLOT_X)
        assertTrue(rightEnd <= P1DisplaySpec.LCD_WIDTH_PX)
    }

    @Test
    fun visitSlots_centerSpritesInEachHalf() {
        val zoneTop = P1DisplaySpec.PET_ZONE_TOP_PX.toFloat()
        val zoneHeight = P1DisplaySpec.PET_ZONE_HEIGHT_PX.toFloat()
        val slotWidth = P1DisplaySpec.VISIT_SLOT_WIDTH_PX.toFloat()

        val left = computeSpriteDstRect(
            zoneLeftPx = P1DisplaySpec.VISIT_LEFT_SLOT_X.toFloat(),
            zoneTopPx = zoneTop,
            zoneWidthPx = slotWidth,
            zoneHeightPx = zoneHeight,
        )
        val leftCenterX = left.left + left.spriteW / 2f
        val leftSlotCenter = P1DisplaySpec.VISIT_LEFT_SLOT_X + slotWidth / 2f
        assertEquals(leftSlotCenter, leftCenterX, 0.01f)

        val right = computeSpriteDstRect(
            zoneLeftPx = P1DisplaySpec.VISIT_RIGHT_SLOT_X.toFloat(),
            zoneTopPx = zoneTop,
            zoneWidthPx = slotWidth,
            zoneHeightPx = zoneHeight,
        )
        val rightCenterX = right.left + right.spriteW / 2f
        val rightSlotCenter = P1DisplaySpec.VISIT_RIGHT_SLOT_X + slotWidth / 2f
        assertEquals(rightSlotCenter, rightCenterX, 0.01f)
    }
}
