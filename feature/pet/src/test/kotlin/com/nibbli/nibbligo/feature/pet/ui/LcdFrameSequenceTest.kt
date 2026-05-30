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
    fun eatingCyclesEightFrames() {
        val selection = SpriteSelection(
            NibbliSpriteAtlas.Frame.EATING_A,
            NibbliSpriteAtlas.Frame.EATING_B,
        )
        val sequence = selection.toSequence()
        assertEquals(8, sequence.frames.size)
        assertEquals(165L, sequence.stepMs)
        assertEquals(NibbliSpriteAtlas.Frame.EATING_B, frameAt(sequence, 1))
        assertEquals(NibbliSpriteAtlas.Frame.HAPPY, frameAt(sequence, 6))
    }

    @Test
    fun playfulUsesLongerCycle() {
        val selection = SpriteSelection(
            NibbliSpriteAtlas.Frame.PLAYFUL,
            NibbliSpriteAtlas.Frame.HAPPY,
        )
        assertEquals(8, selection.lcdFrameSequence().size)
        val sequence = selection.toSequence()
        assertEquals(175L, sequence.stepMs)
        assertEquals(NibbliSpriteAtlas.Frame.HAPPY, frameAt(sequence, 1))
        assertEquals(NibbliSpriteAtlas.Frame.HAPPY, frameAt(sequence, 7))
    }
}
