package com.nibbli.nibbligo.core.designsystem.theme

import com.nibbli.nibbligo.core.model.AppAccentPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class AccentColorsTest {
    @Test
    fun lightPrimaryDistinctPerPalette() {
        val primaries = AppAccentPalette.entries.map {
            AccentColors.tokens(it, ThemeBase.LIGHT).primary.value
        }
        assertEquals(primaries.size, primaries.toSet().size)
    }

    @Test
    fun swatchColorMatchesLightPrimaryApproximation() {
        assertEquals(
            AccentColors.tokens(AppAccentPalette.TEAL, ThemeBase.LIGHT).primary,
            AccentColors.swatchColor(AppAccentPalette.TEAL),
        )
    }
}
