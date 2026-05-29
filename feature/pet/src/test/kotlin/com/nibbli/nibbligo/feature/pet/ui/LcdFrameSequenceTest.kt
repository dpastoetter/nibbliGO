package com.nibbli.nibbligo.feature.pet.ui

import com.nibbli.nibbligo.feature.pet.ui.pixel.NibbliSpriteAtlas
import com.nibbli.nibbligo.feature.pet.ui.pixel.SpriteSelection
import com.nibbli.nibbligo.feature.pet.ui.pixel.frameAt
import com.nibbli.nibbligo.feature.pet.ui.pixel.lcdFrameSequence
import com.nibbli.nibbligo.feature.pet.ui.pixel.toSequence
import org.junit.Assert.assertEquals
import org.junit.Test

class LcdFrameSequenceTest {

    @Test
    fun eatingCyclesFourFrames() {
        val selection = SpriteSelection(
            NibbliSpriteAtlas.Frame.EATING_A,
            NibbliSpriteAtlas.Frame.EATING_B,
        )
        val sequence = selection.toSequence()
        assertEquals(4, sequence.frames.size)
        assertEquals(220L, sequence.stepMs)
        assertEquals(NibbliSpriteAtlas.Frame.EATING_B, frameAt(sequence, 1))
        assertEquals(NibbliSpriteAtlas.Frame.EATING_A, frameAt(sequence, 2))
    }

    @Test
    fun playfulUsesLongerCycle() {
        val selection = SpriteSelection(
            NibbliSpriteAtlas.Frame.PLAYFUL,
            NibbliSpriteAtlas.Frame.HAPPY,
        )
        assertEquals(5, selection.lcdFrameSequence().size)
        val sequence = selection.toSequence()
        assertEquals(210L, sequence.stepMs)
        assertEquals(NibbliSpriteAtlas.Frame.HAPPY, frameAt(sequence, 1))
        assertEquals(NibbliSpriteAtlas.Frame.IDLE_B, frameAt(sequence, 4))
    }
}
