package com.nibbli.nibbligo.core.pet.llm

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState

object PetMoodDescriber {
    fun describe(state: PetState): String = buildString {
        when (state.condition) {
            PetCondition.SLEEPING -> append("sleepy and drowsy")
            PetCondition.SICK -> append("under the weather")
            PetCondition.DEAD -> append("gone")
            PetCondition.HEALTHY -> when {
                state.stats.mood >= 80 && state.stats.energy >= 60 -> append("cheerful and bouncy")
                state.stats.mood >= 60 -> append("content and cozy")
                state.stats.mood >= 40 -> append("a little restless")
                else -> append("glum and needing attention")
            }
        }
        when (state.activeNeed) {
            PetNeed.HUNGRY -> append(", tummy rumbling")
            PetNeed.TIRED -> append(", low on energy")
            PetNeed.DIRTY -> append(", wishing for a tidy room")
            PetNeed.SICK -> append(", not feeling great")
            PetNeed.UNHAPPY -> append(", wanting company")
            PetNeed.LONELY -> append(", missing you")
            PetNeed.NONE -> Unit
        }
        append("; expression ${state.expression.name.lowercase()}")
        if (state.stage == LifeStage.EGG) append("; still an egg but curious inside")
    }

    fun templateLine(state: PetState): String = when {
        state.condition == PetCondition.SLEEPING -> listOf(
            "Zzz… dreaming of on-device adventures…",
            "So cozy… just five more minutes…",
        ).random()
        state.condition == PetCondition.SICK -> listOf(
            "Achoo… a little rest would help…",
            "Feeling woozy… stay close?",
        ).random()
        state.stage == LifeStage.EGG -> listOf(
            "…wiggle… something's happening in here!",
            "Tiny taps from inside the shell…",
        ).random()
        state.stats.hunger < 35 -> listOf(
            "Is that a snack I smell?",
            "My pixel tummy is doing a tiny rumble.",
        ).random()
        state.stats.mood >= 75 -> listOf(
            "Today feels sparkly on this little screen!",
            "Boop! I'm in such a good mood!",
            "I could bounce around this LCD all day!",
        ).random()
        state.stats.energy < 30 -> listOf(
            "Yawn… maybe a nap soon?",
            "My batteries are cute but low.",
        ).random()
        state.expression == PetExpression.CURIOUS -> listOf(
            "I wonder what we could chat about on-device…",
            "So many buttons to peek at!",
        ).random()
        else -> listOf(
            "Just hanging out in your pocket.",
            "Beep… I'm here if you need me.",
            "This green screen is home sweet home.",
        ).random()
    }
}
