package com.nibbli.nibbligo.feature.pet.ui.pixel

import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetState

data class P1StatIconLayout(
    val startXPx: Float,
    val iconScale: Float,
    val iconCount: Int,
)

fun buildP1StatIcons(pet: PetState): List<Pair<P1StatIcon, Boolean>> = buildList {
    add(P1StatIcon.HUNGER to (pet.stats.hunger < 45))
    add(P1StatIcon.HAPPY to (pet.stats.happiness < 45))
    add(P1StatIcon.ENERGY to (pet.stats.energy < 30))
    if (pet.hasMess) add(P1StatIcon.MESS to true)
    if (pet.condition == PetCondition.SICK || pet.stats.health < 35) {
        add(P1StatIcon.SICK to true)
    }
}

fun computeP1StatIconLayout(
    iconCount: Int,
    talkLcdMode: Boolean,
): P1StatIconLayout {
    val count = iconCount.coerceAtLeast(0)
    if (count == 0) {
        return P1StatIconLayout(startXPx = 0f, iconScale = P1DisplaySpec.ICON_SCALE, iconCount = 0)
    }

    val gapPx = P1DisplaySpec.BOTTOM_STRIP_GAP_PX.toFloat()
    val availablePx = if (talkLcdMode) {
        (P1DisplaySpec.LCD_WIDTH_PX - P1DisplaySpec.TALK_STAT_ICONS_START_PX - 2).toFloat()
    } else {
        (P1DisplaySpec.LCD_WIDTH_PX - 4).toFloat()
    }

    var scale = P1DisplaySpec.ICON_SCALE
    var totalWidth = statStripWidthPx(count, scale, gapPx)
    while (totalWidth > availablePx && scale > 0.55f) {
        scale -= 0.05f
        totalWidth = statStripWidthPx(count, scale, gapPx)
    }

    val startXPx = if (talkLcdMode) {
        P1DisplaySpec.TALK_STAT_ICONS_START_PX.toFloat()
    } else {
        ((P1DisplaySpec.LCD_WIDTH_PX - totalWidth) / 2f).coerceAtLeast(0f)
    }

    return P1StatIconLayout(
        startXPx = startXPx,
        iconScale = scale,
        iconCount = count,
    )
}

private fun statStripWidthPx(iconCount: Int, iconScale: Float, gapPx: Float): Float {
    if (iconCount <= 0) return 0f
    val iconPx = P1DisplaySpec.ICON_SIZE_PX * iconScale
    return iconCount * iconPx + (iconCount - 1) * gapPx
}
