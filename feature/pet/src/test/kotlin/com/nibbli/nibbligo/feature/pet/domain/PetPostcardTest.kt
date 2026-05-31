package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.pixel.NibbliSpriteAtlas
import com.nibbli.nibbligo.feature.pet.ui.pixel.resolveSprite
import org.junit.Assert.assertEquals
import org.junit.Test

class PetPostcardTest {

    @Test
    fun toVisitDisplayState_playfulWhenMoodHigh() {
        val postcard = PetPostcard(
            friendCode = "abc123",
            senderName = "Guest",
            stage = LifeStage.CHILD,
            careScore = 50,
            dialogueLine = "Hi!",
            mood = 88,
            equippedCosmetic = null,
        )
        val state = postcard.toVisitDisplayState()
        assertEquals(PetAnimation.PLAY, state.animation)
        assertEquals(NibbliSpriteAtlas.Frame.PLAYFUL, state.resolveSprite().primary)
    }

    @Test
    fun forVisitPlaydate_nudgesIdlePetToPlay() {
        val idle = PetState(condition = PetCondition.HEALTHY)
        assertEquals(PetAnimation.PLAY, idle.forVisitPlaydate().animation)
    }

    @Test
    fun forVisitPlaydate_leavesSleepingUnchanged() {
        val sleeping = PetState(
            condition = PetCondition.SLEEPING,
            animation = PetAnimation.SLEEP,
        )
        assertEquals(PetAnimation.SLEEP, sleeping.forVisitPlaydate().animation)
    }
}
