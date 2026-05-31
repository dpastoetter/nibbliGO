package com.nibbli.nibbligo.feature.pet.ui.pixel

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.core.model.equippedScene
import com.nibbli.nibbligo.feature.pet.ui.pixel.LcdPickerEntry
import com.nibbli.nibbligo.feature.pet.ui.pixel.applyPickerEquip
import com.nibbli.nibbligo.feature.pet.ui.pixel.isCurrentlyEquipped
import com.nibbli.nibbligo.feature.pet.ui.pixel.lcdItemPickerEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetLcdItemCatalogTest {

    private val basePet = PetState(
        unlockedCosmetics = setOf(PetCosmetic.SPARKLE_COLLAR),
        unlockedScenes = setOf(PetLcdScene.COZY, PetLcdScene.STARS),
        unlockedProps = setOf(PetLcdProp.BALL),
    )

    @Test
    fun picker_includes_none_locked_and_unlocked_entries() {
        val entries = basePet.lcdItemPickerEntries()
        assertTrue(entries.any { it is LcdPickerEntry.WearNone })
        assertTrue(entries.any { it is LcdPickerEntry.Wearable })
        assertTrue(entries.any { it is LcdPickerEntry.LockedPreview })
        assertTrue(entries.any { it is LcdPickerEntry.Scene })
        assertTrue(entries.any { it is LcdPickerEntry.Prop })
    }

    @Test
    fun equip_wearable_and_scene_and_prop() {
        val wear = basePet.applyPickerEquip(LcdPickerEntry.Wearable(PetCosmetic.SPARKLE_COLLAR))
        assertEquals(PetCosmetic.SPARKLE_COLLAR, wear?.equippedCosmetic)

        val scene = basePet.applyPickerEquip(LcdPickerEntry.Scene(PetLcdScene.STARS))
        assertEquals(PetLcdScene.STARS, scene?.equippedScene)

        val prop = basePet.applyPickerEquip(LcdPickerEntry.Prop(PetLcdProp.BALL))
        assertEquals(PetLcdProp.BALL, prop?.equippedProp)
    }

    @Test
    fun locked_entry_cannot_equip() {
        assertNull(basePet.applyPickerEquip(LcdPickerEntry.LockedPreview("LOCKED")))
    }

    @Test
    fun isCurrentlyEquipped_reflects_state() {
        val equipped = basePet.copy(
            equippedCosmetic = PetCosmetic.SPARKLE_COLLAR,
            roomId = PetLcdScene.STARS.id,
            equippedProp = PetLcdProp.BALL,
        )
        assertTrue(LcdPickerEntry.Wearable(PetCosmetic.SPARKLE_COLLAR).isCurrentlyEquipped(equipped))
        assertTrue(LcdPickerEntry.Scene(PetLcdScene.STARS).isCurrentlyEquipped(equipped))
        assertTrue(LcdPickerEntry.Prop(PetLcdProp.BALL).isCurrentlyEquipped(equipped))
        assertFalse(LcdPickerEntry.WearNone.isCurrentlyEquipped(equipped))
    }
}
