package com.nibbli.nibbligo.core.model

/** Authoritative Pixel Friend game constants shared by simulation and FAQ. */
object PetGameReference {
    const val CRITICAL_HUNGER_THRESHOLD = 10
    const val CRITICAL_HEALTH_THRESHOLD = 15
    const val CRITICAL_DEATH_MS = 4 * 60 * 60 * 1000L

    const val NEED_HUNGRY_THRESHOLD = 25
    const val NEED_DIRTY_HYGIENE_THRESHOLD = 25
    const val NEED_TIRED_ENERGY_THRESHOLD = 20
    const val NEED_UNHAPPY_MOOD_THRESHOLD = 30
    const val LONELY_HOURS = 8

    const val MESS_HYGIENE_THRESHOLD = 35
    const val SICK_HYGIENE_THRESHOLD = 25

    const val CARE_SCORE_TICK_THRESHOLD = 40
    const val CARE_SCORE_PER_INTERACTION = 2

    const val TICK_INTERVAL_MINUTES = 15

    const val MINIGAME_DURATION_SECONDS = 28
    const val MINIGAME_WIN_SCORE = 18

    data class EvolutionGate(
        val minAgeMinutes: Int,
        val minCareScore: Int,
        val ageOrCareScore: Boolean = false,
    )

    fun evolutionGate(from: LifeStage): EvolutionGate? = when (from) {
        LifeStage.EGG -> EvolutionGate(minAgeMinutes = 30, minCareScore = 55, ageOrCareScore = true)
        LifeStage.BABY -> EvolutionGate(minAgeMinutes = 24 * 60, minCareScore = 45)
        LifeStage.CHILD -> EvolutionGate(minAgeMinutes = 3 * 24 * 60, minCareScore = 50)
        LifeStage.TEEN -> EvolutionGate(minAgeMinutes = 5 * 24 * 60, minCareScore = 55)
        LifeStage.ADULT -> null
    }

    fun canEvolve(state: PetState): LifeStage? {
        val gate = evolutionGate(state.stage) ?: return null
        val ageOk = state.ageMinutes >= gate.minAgeMinutes
        val careOk = state.careScore >= gate.minCareScore
        return when {
            gate.ageOrCareScore && (ageOk || careOk) -> nextStage(state.stage)
            ageOk && careOk -> nextStage(state.stage)
            else -> null
        }
    }

    private fun nextStage(from: LifeStage): LifeStage? = when (from) {
        LifeStage.EGG -> LifeStage.BABY
        LifeStage.BABY -> LifeStage.CHILD
        LifeStage.CHILD -> LifeStage.TEEN
        LifeStage.TEEN -> LifeStage.ADULT
        LifeStage.ADULT -> null
    }

    fun formatEvolutionSummary(): String = buildString {
        append("Evolution: Egg→Baby at 30 min or care 55+; ")
        append("Baby→Child at 24h and care 45+; ")
        append("Child→Teen at 3 days and care 50+; ")
        append("Teen→Adult at 5 days and care 55+.")
    }

    fun formatCosmeticsSummary(): String = PetCosmetic.entries.joinToString(" ") { cosmetic ->
        "${cosmetic.name.replace('_', ' ').lowercase()}: skill ${cosmetic.unlockSkill}+ trust ${cosmetic.unlockTrust}+"
    }

    fun formatLcdItemsSummary(): String = buildString {
        append("LCD Items menu on device: cycle wearables, scenes, and floor props. ")
        append(formatCosmeticsSummary())
        append(" Scenes: stars care 60+, clouds teen+, night adult care 75+. ")
        append("Props: ball, plant, mat unlock from arcade wins and daily quest bonus.")
    }
}
