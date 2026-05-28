package com.nibbli.nibbligo.core.model

data class PetStats(
    val hunger: Int = 70,
    val energy: Int = 80,
    val mood: Int = 75,
    val hygiene: Int = 80,
    val health: Int = 90,
    val discipline: Int = 50,
    val weight: Int = 50,
    val trust: Int = 50,
    val curiosity: Int = 40,
    val skill: Int = 10,
) {
    val happiness: Int get() = mood

    fun clamped(): PetStats = copy(
        hunger = hunger.coerceIn(0, 100),
        energy = energy.coerceIn(0, 100),
        mood = mood.coerceIn(0, 100),
        hygiene = hygiene.coerceIn(0, 100),
        health = health.coerceIn(0, 100),
        discipline = discipline.coerceIn(0, 100),
        weight = weight.coerceIn(0, 100),
        trust = trust.coerceIn(0, 100),
        curiosity = curiosity.coerceIn(0, 100),
        skill = skill.coerceIn(0, 100),
    )
}

enum class LifeStage {
    EGG,
    BABY,
    CHILD,
    TEEN,
    ADULT,
}

enum class PetCondition {
    HEALTHY,
    SICK,
    SLEEPING,
    DEAD,
}

enum class PetNeed {
    NONE,
    HUNGRY,
    UNHAPPY,
    DIRTY,
    TIRED,
    SICK,
    LONELY,
}

enum class PetExpression {
    NEUTRAL,
    HAPPY,
    SLEEPY,
    CURIOUS,
    HUNGRY,
    PROUD,
    SICK,
    ATTENTION,
}

enum class PetAnimation {
    IDLE,
    EAT,
    SLEEP,
    SICK,
    HAPPY,
    ATTENTION,
    EVOLVE,
    PLAY,
}

enum class PetInteraction {
    FEED_MEAL,
    FEED_SNACK,
    PLAY,
    CLEAN,
    MEDICINE,
    SLEEP,
    WAKE,
    TALK,
    TRAIN,
    GUIDE,
}

enum class PetCosmetic(val unlockSkill: Int, val unlockTrust: Int) {
    SPARKLE_COLLAR(15, 20),
    STAR_PATCH(30, 40),
    AURORA_AURA(50, 60),
}

enum class PetPersonality {
    PLAYFUL,
    CALM,
    CURIOUS,
}

data class PetState(
    val name: String = "nibbli",
    val stats: PetStats = PetStats(),
    val stage: LifeStage = LifeStage.BABY,
    val condition: PetCondition = PetCondition.HEALTHY,
    val activeNeed: PetNeed = PetNeed.NONE,
    val expression: PetExpression = PetExpression.NEUTRAL,
    val animation: PetAnimation = PetAnimation.IDLE,
    val equippedCosmetic: PetCosmetic? = null,
    val lastInteractionAtMillis: Long = System.currentTimeMillis(),
    val lastTickAtMillis: Long = System.currentTimeMillis(),
    val bornAtMillis: Long = System.currentTimeMillis(),
    val ageMinutes: Long = 0,
    val dialogueLine: String = "Hello! I'm nibbli, your pocket AI friend.",
    val memorySummary: String = "",
    val unlockedCosmetics: Set<PetCosmetic> = emptySet(),
    val hasMess: Boolean = false,
    val roomId: String = "cozy",
    val careScore: Int = 50,
    val criticalNeglectSinceMillis: Long? = null,
) {
    val isAlive: Boolean get() = condition != PetCondition.DEAD
}

data class PetTickResult(
    val state: PetState,
    val templateDialogue: String? = null,
    val shouldNotifyAttention: Boolean = false,
    val welcomeBack: Boolean = false,
    val evolved: Boolean = false,
)

data class PetInteractionResult(
    val state: PetState,
    val templateDialogue: String,
)
