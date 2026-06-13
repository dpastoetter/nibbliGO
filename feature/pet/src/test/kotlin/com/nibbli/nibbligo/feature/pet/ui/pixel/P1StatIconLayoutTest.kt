package com.nibbli.nibbligo.feature.pet.ui.pixel

import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetState
import org.junit.Assert.assertTrue
import org.junit.Test

class P1StatIconLayoutTest {

    @Test
    fun talkMode_fitsAllFiveIcons() {
        val layout = computeP1StatIconLayout(iconCount = 5, talkLcdMode = true)
        val gapPx = P1DisplaySpec.BOTTOM_STRIP_GAP_PX.toFloat()
        val iconPx = P1DisplaySpec.ICON_SIZE_PX * layout.iconScale
        val totalWidth = 5 * iconPx + 4 * gapPx
        val endX = layout.startXPx + totalWidth
        assertTrue(endX <= P1DisplaySpec.LCD_WIDTH_PX)
        assertTrue(layout.iconScale > 0.55f)
    }

    @Test
    fun idleMode_centersStrip() {
        val layout = computeP1StatIconLayout(iconCount = 4, talkLcdMode = false)
        val gapPx = P1DisplaySpec.BOTTOM_STRIP_GAP_PX.toFloat()
        val iconPx = P1DisplaySpec.ICON_SIZE_PX * layout.iconScale
        val totalWidth = 4 * iconPx + 3 * gapPx
        val expectedStart = (P1DisplaySpec.LCD_WIDTH_PX - totalWidth) / 2f
        assertTrue(kotlin.math.abs(layout.startXPx - expectedStart) < 0.01f)
    }

    @Test
    fun buildIcons_includesMessAndSickWhenNeeded() {
        val pet = PetState(
            stats = com.nibbli.nibbligo.core.model.PetStats(
                hunger = 10,
                mood = 10,
                energy = 10,
                health = 10,
            ),
            condition = PetCondition.SICK,
            hasMess = true,
        )
        assertTrue(buildP1StatIcons(pet).size >= 5)
    }
}
