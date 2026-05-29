package com.nibbli.nibbligo.feature.pet.ui

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.feature.pet.ui.pixel.NibbliSpriteAtlas
import com.nibbli.nibbligo.feature.pet.ui.pixel.resolveSprite
import com.nibbli.nibbligo.feature.pet.ui.pixel.showsCosmeticOverlay
import com.nibbli.nibbligo.feature.pet.ui.pixel.toOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NibbliSpriteAtlasTest {

    @Test
    fun deadPetUsesDeadFrame() {
        val pet = basePet.copy(condition = PetCondition.DEAD)
        assertEquals(NibbliSpriteAtlas.Frame.DEAD, pet.resolveSprite().primary)
    }

    @Test
    fun eggUsesEggFrame() {
        val pet = basePet.copy(stage = LifeStage.EGG)
        assertEquals(NibbliSpriteAtlas.Frame.EGG, pet.resolveSprite().primary)
    }

    @Test
    fun sleepingUsesSleepingFrame() {
        val pet = basePet.copy(condition = PetCondition.SLEEPING)
        assertEquals(NibbliSpriteAtlas.Frame.SLEEPING, pet.resolveSprite().primary)
    }

    @Test
    fun sickUsesSickFrame() {
        val pet = basePet.copy(condition = PetCondition.SICK)
        assertEquals(NibbliSpriteAtlas.Frame.SICK, pet.resolveSprite().primary)
    }

    @Test
    fun lowHealthUsesSickFrame() {
        val pet = basePet.copy(stats = PetStats(health = 20))
        assertEquals(NibbliSpriteAtlas.Frame.SICK, pet.resolveSprite().primary)
    }

    @Test
    fun eatingUsesAnimatedEatingFrames() {
        val pet = basePet.copy(animation = PetAnimation.EAT)
        val selection = pet.resolveSprite()
        assertEquals(NibbliSpriteAtlas.Frame.EATING_A, selection.primary)
        assertEquals(NibbliSpriteAtlas.Frame.EATING_B, selection.alternate)
    }

    @Test
    fun activeNeedUsesAttentionFrame() {
        val pet = basePet.copy(activeNeed = PetNeed.HUNGRY)
        assertEquals(NibbliSpriteAtlas.Frame.ATTENTION, pet.resolveSprite().primary)
    }

    @Test
    fun hungryUsesHungryFrameWhenNoActiveNeed() {
        val pet = basePet.copy(
            expression = PetExpression.HUNGRY,
            stats = PetStats(hunger = 20),
            activeNeed = PetNeed.NONE,
        )
        assertEquals(NibbliSpriteAtlas.Frame.HUNGRY, pet.resolveSprite().primary)
    }

    @Test
    fun playfulUsesPlayfulFrame() {
        val pet = basePet.copy(animation = PetAnimation.PLAY)
        assertEquals(NibbliSpriteAtlas.Frame.PLAYFUL, pet.resolveSprite().primary)
    }

    @Test
    fun happyUsesHappyFrame() {
        val pet = basePet.copy(expression = PetExpression.HAPPY)
        assertEquals(NibbliSpriteAtlas.Frame.HAPPY, pet.resolveSprite().primary)
    }

    @Test
    fun idleUsesAnimatedBlinkFrames() {
        val pet = basePet
        val selection = pet.resolveSprite()
        assertEquals(NibbliSpriteAtlas.Frame.IDLE_A, selection.primary)
        assertEquals(NibbliSpriteAtlas.Frame.IDLE_B, selection.alternate)
    }

    @Test
    fun cosmeticMapsToOverlayColumn() {
        assertEquals(
            NibbliSpriteAtlas.CosmeticOverlay.STAR_PATCH,
            PetCosmetic.STAR_PATCH.toOverlay(),
        )
    }

    @Test
    fun moodPulseAwakeWhenHealthyIdle() {
        assertTrue(basePet.isAwakeForMoodPulse)
    }

    @Test
    fun moodPulseNotAwakeWhenSleeping() {
        assertFalse(basePet.copy(condition = PetCondition.SLEEPING).isAwakeForMoodPulse)
        assertFalse(basePet.copy(animation = PetAnimation.SLEEP).isAwakeForMoodPulse)
    }

    @Test
    fun cosmeticOverlayHiddenOnEggAndDead() {
        val equipped = basePet.copy(equippedCosmetic = PetCosmetic.AURORA_AURA)
        assertEquals(false, equipped.copy(stage = LifeStage.EGG).showsCosmeticOverlay())
        assertEquals(false, equipped.copy(condition = PetCondition.DEAD).showsCosmeticOverlay())
        assertEquals(true, equipped.showsCosmeticOverlay())
    }

    private companion object {
        val basePet = PetState(
            stage = LifeStage.BABY,
            condition = PetCondition.HEALTHY,
            activeNeed = PetNeed.NONE,
        )
    }
}
