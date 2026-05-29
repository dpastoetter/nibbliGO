package com.nibbli.nibbligo.feature.pet.ui

import androidx.annotation.DrawableRes
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.R

@get:DrawableRes
val PetState.portraitRes: Int
    get() = when {
        condition == PetCondition.DEAD -> R.drawable.nibbli_dead
        stage == LifeStage.EGG -> R.drawable.nibbli_egg
        condition == PetCondition.SLEEPING -> R.drawable.nibbli_sleeping
        condition == PetCondition.SICK || stats.health < 30 -> R.drawable.nibbli_sick
        animation == PetAnimation.EAT ||
            expression == PetExpression.HUNGRY ||
            stats.hunger < 35 -> R.drawable.nibbli_hungry
        animation == PetAnimation.PLAY || animation == PetAnimation.HAPPY -> R.drawable.nibbli_playful
        expression == PetExpression.HAPPY || expression == PetExpression.PROUD -> R.drawable.nibbli_happy
        else -> R.drawable.nibbli_idle
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
