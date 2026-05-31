package com.nibbli.nibbligo.feature.pet.ui.pixel

data class SpriteSequence(
    val frames: List<NibbliSpriteAtlas.Frame>,
    val stepMs: Long,
)

fun SpriteSelection.toSequence(): SpriteSequence {
    val frames = lcdFrameSequence()
    return SpriteSequence(frames = frames, stepMs = frameIntervalMs())
}

fun frameAt(sequence: SpriteSequence, frameIndex: Int): NibbliSpriteAtlas.Frame {
    val size = sequence.frames.size.coerceAtLeast(1)
    return sequence.frames[frameIndex.mod(size)]
}

/** Multi-frame LCD sprite cycles — faster and longer than a simple A/B ping-pong. */
fun SpriteSelection.lcdFrameSequence(): List<NibbliSpriteAtlas.Frame> {
    val alt = alternate ?: primary
    return when (primary) {
        NibbliSpriteAtlas.Frame.EATING_A -> listOf(
            NibbliSpriteAtlas.Frame.EATING_A,
            NibbliSpriteAtlas.Frame.EATING_B,
            NibbliSpriteAtlas.Frame.EATING_A,
            NibbliSpriteAtlas.Frame.EATING_B,
            NibbliSpriteAtlas.Frame.EATING_A,
            NibbliSpriteAtlas.Frame.EATING_B,
            NibbliSpriteAtlas.Frame.HAPPY,
            NibbliSpriteAtlas.Frame.EATING_B,
        )
        NibbliSpriteAtlas.Frame.PLAYFUL -> listOf(
            NibbliSpriteAtlas.Frame.PLAYFUL,
            NibbliSpriteAtlas.Frame.HAPPY,
            NibbliSpriteAtlas.Frame.PLAYFUL,
            NibbliSpriteAtlas.Frame.IDLE_B,
            NibbliSpriteAtlas.Frame.PLAYFUL,
            NibbliSpriteAtlas.Frame.HAPPY,
            NibbliSpriteAtlas.Frame.PLAYFUL,
            NibbliSpriteAtlas.Frame.HAPPY,
        )
        NibbliSpriteAtlas.Frame.HAPPY -> listOf(
            NibbliSpriteAtlas.Frame.HAPPY,
            NibbliSpriteAtlas.Frame.PLAYFUL,
            NibbliSpriteAtlas.Frame.HAPPY,
            NibbliSpriteAtlas.Frame.IDLE_B,
            NibbliSpriteAtlas.Frame.HAPPY,
            NibbliSpriteAtlas.Frame.PLAYFUL,
        )
        NibbliSpriteAtlas.Frame.ATTENTION -> listOf(
            NibbliSpriteAtlas.Frame.ATTENTION,
            NibbliSpriteAtlas.Frame.IDLE_A,
            NibbliSpriteAtlas.Frame.ATTENTION,
            NibbliSpriteAtlas.Frame.ATTENTION,
            NibbliSpriteAtlas.Frame.IDLE_B,
            NibbliSpriteAtlas.Frame.ATTENTION,
        )
        NibbliSpriteAtlas.Frame.IDLE_A -> listOf(
            NibbliSpriteAtlas.Frame.IDLE_A,
            NibbliSpriteAtlas.Frame.IDLE_B,
            NibbliSpriteAtlas.Frame.IDLE_A,
            NibbliSpriteAtlas.Frame.HAPPY,
            NibbliSpriteAtlas.Frame.IDLE_B,
        )
        NibbliSpriteAtlas.Frame.HUNGRY -> listOf(
            NibbliSpriteAtlas.Frame.HUNGRY,
            NibbliSpriteAtlas.Frame.IDLE_A,
            NibbliSpriteAtlas.Frame.HUNGRY,
            NibbliSpriteAtlas.Frame.HUNGRY,
            NibbliSpriteAtlas.Frame.IDLE_B,
            NibbliSpriteAtlas.Frame.HUNGRY,
        )
        NibbliSpriteAtlas.Frame.SICK -> listOf(
            NibbliSpriteAtlas.Frame.SICK,
            NibbliSpriteAtlas.Frame.IDLE_A,
            NibbliSpriteAtlas.Frame.SICK,
            NibbliSpriteAtlas.Frame.IDLE_B,
            NibbliSpriteAtlas.Frame.SICK,
        )
        NibbliSpriteAtlas.Frame.SLEEPING -> listOf(NibbliSpriteAtlas.Frame.SLEEPING)
        NibbliSpriteAtlas.Frame.EGG -> listOf(NibbliSpriteAtlas.Frame.EGG)
        NibbliSpriteAtlas.Frame.DEAD -> listOf(NibbliSpriteAtlas.Frame.DEAD)
        else -> listOf(primary, alt, primary)
    }
}

fun SpriteSelection.frameAtIndex(index: Int): NibbliSpriteAtlas.Frame =
    frameAt(toSequence(), index)

fun SpriteSelection.frameIntervalMs(): Long {
    val base = when (primary) {
        NibbliSpriteAtlas.Frame.EATING_A -> 165L
        NibbliSpriteAtlas.Frame.PLAYFUL -> 175L
        NibbliSpriteAtlas.Frame.ATTENTION -> 200L
        NibbliSpriteAtlas.Frame.IDLE_A -> 280L
        NibbliSpriteAtlas.Frame.HAPPY -> 220L
        NibbliSpriteAtlas.Frame.HUNGRY -> 300L
        NibbliSpriteAtlas.Frame.SICK -> 420L
        NibbliSpriteAtlas.Frame.SLEEPING -> 720L
        NibbliSpriteAtlas.Frame.EGG -> 340L
        else -> 300L
    }
    return if (primary == NibbliSpriteAtlas.Frame.SLEEPING) {
        base
    } else {
        (base * LcdAwakeMotionTuning.FRAME_INTERVAL_SCALE).toLong()
    }
}
