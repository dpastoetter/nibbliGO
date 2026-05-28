package com.nibbli.nibbligo.core.model

data class PetStats(
    val hunger: Int = 70,
    val energy: Int = 80,
    val mood: Int = 75,
    val trust: Int = 50,
    val curiosity: Int = 40,
    val skill: Int = 10,
) {
    fun clamped(): PetStats = copy(
        hunger = hunger.coerceIn(0, 100),
        energy = energy.coerceIn(0, 100),
        mood = mood.coerceIn(0, 100),
        trust = trust.coerceIn(0, 100),
        curiosity = curiosity.coerceIn(0, 100),
        skill = skill.coerceIn(0, 100),
    )
}

enum class PetExpression {
    NEUTRAL,
    HAPPY,
    SLEEPY,
    CURIOUS,
    HUNGRY,
    PROUD,
}

enum class PetInteraction {
    FEED,
    PLAY,
    REST,
    TRAIN,
    TALK,
}

enum class PetCosmetic(val unlockSkill: Int, val unlockTrust: Int) {
    SPARKLE_COLLAR(15, 20),
    STAR_PATCH(30, 40),
    AURORA_AURA(50, 60),
}

data class PetState(
    val stats: PetStats = PetStats(),
    val expression: PetExpression = PetExpression.NEUTRAL,
    val equippedCosmetic: PetCosmetic? = null,
    val lastInteractionAtMillis: Long = System.currentTimeMillis(),
    val dialogueLine: String = "Hello! I'm nibbli, your pocket AI friend.",
    val unlockedCosmetics: Set<PetCosmetic> = emptySet(),
)
