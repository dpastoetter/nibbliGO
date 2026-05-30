package com.nibbli.nibbligo.feature.pet.ui.pixel

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState

/** Original nibbli sprite art — not derived from third-party Tamagotchi ROMs. */
object NibbliSpriteAtlas {
    const val FRAME_SIZE_PX = 32
    const val COLUMN_COUNT = 12
    const val ROW_COUNT = 2
    const val OVERLAY_ROW_PX = FRAME_SIZE_PX

    enum class CosmeticOverlay(val col: Int) {
        SPARKLE_COLLAR(0),
        STAR_PATCH(1),
        AURORA_AURA(2),
    }

    enum class Frame(val col: Int) {
        EGG(0),
        IDLE_A(1),
        IDLE_B(2),
        HAPPY(3),
        HUNGRY(4),
        EATING_A(5),
        EATING_B(6),
        SLEEPING(7),
        SICK(8),
        PLAYFUL(9),
        ATTENTION(10),
        DEAD(11),
    }
}

data class SpriteSelection(
    val primary: NibbliSpriteAtlas.Frame,
    val alternate: NibbliSpriteAtlas.Frame? = null,
)

fun PetState.resolveSprite(): SpriteSelection = when {
    condition == PetCondition.DEAD -> SpriteSelection(NibbliSpriteAtlas.Frame.DEAD)
    stage == LifeStage.EGG -> SpriteSelection(NibbliSpriteAtlas.Frame.EGG)
    condition == PetCondition.SLEEPING || animation == PetAnimation.SLEEP ->
        SpriteSelection(NibbliSpriteAtlas.Frame.SLEEPING)
    condition == PetCondition.SICK || stats.health < 30 ->
        SpriteSelection(NibbliSpriteAtlas.Frame.SICK, NibbliSpriteAtlas.Frame.IDLE_A)
    animation == PetAnimation.EAT ->
        SpriteSelection(NibbliSpriteAtlas.Frame.EATING_A, NibbliSpriteAtlas.Frame.EATING_B)
    animation == PetAnimation.EVOLVE ->
        SpriteSelection(NibbliSpriteAtlas.Frame.PLAYFUL, NibbliSpriteAtlas.Frame.HAPPY)
    animation == PetAnimation.ATTENTION || activeNeed != PetNeed.NONE ->
        SpriteSelection(NibbliSpriteAtlas.Frame.ATTENTION, NibbliSpriteAtlas.Frame.IDLE_A)
    expression == PetExpression.HUNGRY || stats.hunger < 35 ->
        SpriteSelection(NibbliSpriteAtlas.Frame.HUNGRY, NibbliSpriteAtlas.Frame.IDLE_A)
    animation == PetAnimation.PLAY || animation == PetAnimation.HAPPY ->
        SpriteSelection(NibbliSpriteAtlas.Frame.PLAYFUL, NibbliSpriteAtlas.Frame.HAPPY)
    expression == PetExpression.HAPPY || expression == PetExpression.PROUD ->
        SpriteSelection(NibbliSpriteAtlas.Frame.HAPPY, NibbliSpriteAtlas.Frame.PLAYFUL)
    stats.mood >= 75 ->
        SpriteSelection(NibbliSpriteAtlas.Frame.HAPPY, NibbliSpriteAtlas.Frame.IDLE_B)
    stats.mood >= 55 ->
        SpriteSelection(NibbliSpriteAtlas.Frame.IDLE_A, NibbliSpriteAtlas.Frame.HAPPY)
    else -> SpriteSelection(NibbliSpriteAtlas.Frame.IDLE_A, NibbliSpriteAtlas.Frame.IDLE_B)
}

fun needIcon(need: PetNeed): String = when (need) {
    PetNeed.HUNGRY -> "🍗"
    PetNeed.DIRTY -> "🧹"
    PetNeed.TIRED -> "💤"
    PetNeed.SICK -> "💊"
    PetNeed.UNHAPPY -> "💧"
    PetNeed.LONELY -> "💬"
    else -> "❗"
}

fun PetCosmetic.toOverlay(): NibbliSpriteAtlas.CosmeticOverlay = when (this) {
    PetCosmetic.SPARKLE_COLLAR -> NibbliSpriteAtlas.CosmeticOverlay.SPARKLE_COLLAR
    PetCosmetic.STAR_PATCH -> NibbliSpriteAtlas.CosmeticOverlay.STAR_PATCH
    PetCosmetic.AURORA_AURA -> NibbliSpriteAtlas.CosmeticOverlay.AURORA_AURA
}

/** Cosmetics composite on the creature, not on egg or dead poses. */
fun PetState.showsCosmeticOverlay(): Boolean {
    if (equippedCosmetic == null) return false
    if (condition == PetCondition.DEAD || stage == LifeStage.EGG) return false
    return true
}
