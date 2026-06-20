package com.nibbli.nibbligo.feature.pet.ui.collection

import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.pixel.LcdPickerEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetLcdCollectionMetadataTest {

    private val basePet = PetState(
        name = "nibbli",
        unlockedCosmetics = setOf(PetCosmetic.SPARKLE_COLLAR),
        unlockedScenes = setOf(PetLcdScene.COZY, PetLcdScene.STARS),
        unlockedProps = setOf(PetLcdProp.BALL),
        equippedCosmetic = PetCosmetic.SPARKLE_COLLAR,
        roomId = PetLcdScene.STARS.id,
        equippedProp = PetLcdProp.BALL,
    )

    @Test
    fun lcdCollectionEntries_returnsTenItemsWithRefs() {
        val entries = basePet.lcdCollectionEntries()
        assertEquals(10, entries.size)
        assertEquals(3, entries.count { it.category == "Wearable" })
        assertEquals(4, entries.count { it.category == "Scene" })
        assertEquals(3, entries.count { it.category == "Floor prop" })
        assertTrue(entries.all { it.id.isNotBlank() && it.displayName.isNotBlank() })
        assertTrue(entries.any { it.itemRef is LcdCollectionItemRef.Wearable })
        assertTrue(entries.any { it.itemRef is LcdCollectionItemRef.Scene })
        assertTrue(entries.any { it.itemRef is LcdCollectionItemRef.Prop })
    }

    @Test
    fun toPickerEntry_returnsNullWhenLocked() {
        val locked = basePet.lcdCollectionEntries().first { !it.unlocked }
        assertNull(locked.toPickerEntry())
    }

    @Test
    fun toPickerEntry_returnsPickerEntryWhenUnlocked() {
        val unlocked = basePet.lcdCollectionEntries().first { it.unlocked }
        val pickerEntry = unlocked.toPickerEntry()
        assertNotNull(pickerEntry)
        assertFalse(pickerEntry is LcdPickerEntry.LockedPreview)
    }

    @Test
    fun isEquipped_detectsEquippedItems() {
        val entries = basePet.lcdCollectionEntries()
        val collar = entries.first { it.id == "cosmetic_${PetCosmetic.SPARKLE_COLLAR.name}" }
        val stars = entries.first { it.id == "scene_${PetLcdScene.STARS.name}" }
        val ball = entries.first { it.id == "prop_${PetLcdProp.BALL.name}" }
        val locked = entries.first { !it.unlocked }

        assertTrue(collar.isEquipped(basePet))
        assertTrue(stars.isEquipped(basePet))
        assertTrue(ball.isEquipped(basePet))
        assertFalse(locked.isEquipped(basePet))
    }
}
