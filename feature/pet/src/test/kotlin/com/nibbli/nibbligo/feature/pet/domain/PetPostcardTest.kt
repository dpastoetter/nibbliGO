package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.ui.pixel.NibbliSpriteAtlas
import com.nibbli.nibbligo.feature.pet.ui.pixel.resolveSprite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PetPostcardTest {

    @Test
    fun careTipFrom_lowMood() {
        val tip = PetPostcard.careTipFrom(PetState(stats = com.nibbli.nibbligo.core.model.PetStats(mood = 20)))
        assertEquals("They need play!", tip)
    }

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

    @Test
    fun jsonV2_roundTrip() {
        val postcard = PetPostcard(
            friendCode = "ABCD1234",
            senderName = "Guest",
            stage = LifeStage.TEEN,
            careScore = 60,
            dialogueLine = "Hi friend!",
            mood = 70,
            equippedCosmetic = PetCosmetic.STAR_PATCH.name,
            roomId = PetLcdScene.STARS.id,
            equippedProp = PetLcdProp.BALL.id,
            visitMessage = "Come play!",
            careTip = "Feed them!",
            borrowedPropId = PetLcdProp.BALL.id,
            borrowedSceneId = PetLcdScene.STARS.id,
            importedAtMillis = 1_700_000_000_000L,
        )
        val decoded = PetPostcard.fromJson(postcard.toJson())
        assertNotNull(decoded)
        assertEquals(postcard.senderName, decoded!!.senderName)
        assertEquals(postcard.visitMessage, decoded.visitMessage)
        assertEquals(postcard.borrowedPropId, decoded.borrowedPropId)
    }

    @Test
    fun jsonV1_backwardCompatible() {
        val raw = """
            {"v":1,"friendCode":"abc","senderName":"Old","stage":"CHILD","careScore":40,
            "dialogueLine":"Hi","mood":50,"equippedCosmetic":"","roomId":"","equippedProp":""}
        """.trimIndent()
        val decoded = PetPostcard.fromJson(raw)
        assertNotNull(decoded)
        assertEquals("Old", decoded!!.senderName)
    }

    @Test
    fun isExpired_after24Hours() {
        val postcard = PetPostcard(
            friendCode = "abc",
            senderName = "Guest",
            stage = LifeStage.CHILD,
            careScore = 40,
            dialogueLine = "Hi",
            mood = 50,
            equippedCosmetic = null,
            importedAtMillis = 0L,
        )
        assertTrue(PetPostcard.isExpired(postcard, nowMillis = 25 * 60 * 60 * 1000L))
        assertFalse(PetPostcard.isExpired(postcard, nowMillis = 1 * 60 * 60 * 1000L))
    }

    @Test
    fun careTipFrom_prioritizesHungerOverMood() {
        val tip = PetPostcard.careTipFrom(
            PetState(stats = com.nibbli.nibbligo.core.model.PetStats(hunger = 20, mood = 20)),
        )
        assertEquals("They might need feeding!", tip)
    }

    @Test
    fun applyAndRemoveBorrowedSouvenir() {
        val postcard = PetPostcard(
            friendCode = "abc",
            senderName = "Guest",
            stage = LifeStage.CHILD,
            careScore = 40,
            dialogueLine = "Hi",
            mood = 50,
            equippedCosmetic = null,
            borrowedPropId = PetLcdProp.PLANT.id,
            borrowedSceneId = PetLcdScene.NIGHT.id,
        )
        val borrowed = PetState().applyBorrowedSouvenir(postcard)
        assertTrue(borrowed.unlockedProps.contains(PetLcdProp.PLANT))
        assertTrue(borrowed.unlockedScenes.contains(PetLcdScene.NIGHT))
        val restored = borrowed.removeBorrowedSouvenir(postcard)
        assertFalse(restored.unlockedProps.contains(PetLcdProp.PLANT))
        assertFalse(restored.unlockedScenes.contains(PetLcdScene.NIGHT))
    }
}
