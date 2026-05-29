package com.nibbli.nibbligo.core.model

/** Shared pet need rules — used by simulation and talk replies. */
object PetNeedRules {
    fun deriveNeed(state: PetState, nowMillis: Long = System.currentTimeMillis()): PetNeed {
        if (state.condition == PetCondition.DEAD) return PetNeed.NONE
        if (state.condition == PetCondition.SICK) return PetNeed.SICK
        if (state.stats.hunger < 25) return PetNeed.HUNGRY
        if (state.hasMess || state.stats.hygiene < 25) return PetNeed.DIRTY
        if (state.condition == PetCondition.SLEEPING) return PetNeed.NONE
        if (state.stats.energy < 20) return PetNeed.TIRED
        if (state.stats.mood < 30) return PetNeed.UNHAPPY
        val hoursSince = (nowMillis - state.lastInteractionAtMillis) / (1000 * 60 * 60f)
        if (hoursSince > 8) return PetNeed.LONELY
        return PetNeed.NONE
    }

    /** Honest wellbeing answer tied to simulation stats and active need. */
    fun statusReply(state: PetState, nowMillis: Long = System.currentTimeMillis()): String {
        if (state.condition == PetCondition.DEAD) {
            return "I'm gone… hatch a new egg when you're ready."
        }
        if (state.stage == LifeStage.EGG) {
            return "Still an egg! I can't say much yet — keep caring for me."
        }
        if (state.condition == PetCondition.SLEEPING) {
            return "Zzz… I'm sleeping. Wake me when you want to hang out."
        }

        val need = deriveNeed(state, nowMillis)
        val stats = state.stats
        return when (need) {
            PetNeed.HUNGRY -> "Pretty hungry (${stats.hunger}/100) — a Meal would really help!"
            PetNeed.DIRTY -> if (state.hasMess) {
                "It's messy in here — please Clean up for me!"
            } else {
                "Feeling grubby (${stats.hygiene}/100 hygiene) — Clean would help."
            }
            PetNeed.TIRED -> "So tired (${stats.energy}/100 energy) — I could use some Sleep."
            PetNeed.SICK -> "Not feeling well (${stats.health}/100 health) — I need Meds."
            PetNeed.UNHAPPY -> "Kinda blue (${stats.mood}/100 mood) — Play or Talk with me?"
            PetNeed.LONELY -> "I missed you! Come Talk or Play — I've been lonely."
            PetNeed.NONE -> when {
                stats.mood >= 75 && stats.hunger >= 50 && stats.energy >= 40 ->
                    "I'm doing great! Hunger ${stats.hunger}, mood ${stats.mood}, energy ${stats.energy}."
                else ->
                    "I'm okay. Hunger ${stats.hunger}, mood ${stats.mood}, energy ${stats.energy} — nothing urgent."
            }
        }
    }
}
