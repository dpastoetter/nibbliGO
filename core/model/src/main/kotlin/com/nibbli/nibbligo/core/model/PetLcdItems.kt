package com.nibbli.nibbligo.core.model

enum class PetLcdScene(
    val id: String,
    val menuLabel: String,
    val unlockCareScore: Int,
    val unlockStage: LifeStage?,
) {
    COZY("cozy", "COZY", 0, null),
    STARS("stars", "STARS", 60, null),
    CLOUDS("clouds", "CLOUDS", 45, LifeStage.TEEN),
    NIGHT("night", "NIGHT", 75, LifeStage.ADULT),
    ;

    companion object {
        fun fromId(id: String): PetLcdScene? = entries.find { it.id == id }
    }

    fun meetsUnlockThresholds(state: PetState): Boolean {
        if (this == COZY) return true
        val careOk = state.careScore >= unlockCareScore
        val stageOk = unlockStage == null || state.stage.ordinal >= unlockStage.ordinal
        return careOk && stageOk
    }
}

enum class PetLcdProp(val id: String, val menuLabel: String) {
    BALL("ball", "BALL"),
    PLANT("plant", "PLANT"),
    BLANKET("blanket", "MAT"),
    ;

    companion object {
        fun fromId(id: String): PetLcdProp? = entries.find { it.id == id }

        fun nextLocked(current: Set<PetLcdProp>): PetLcdProp? =
            entries.firstOrNull { it !in current }
    }
}

val PetState.equippedScene: PetLcdScene
    get() = PetLcdScene.fromId(roomId) ?: PetLcdScene.COZY

fun PetState.withEquippedScene(scene: PetLcdScene): PetState = copy(roomId = scene.id)

object PetLcdItemUnlocks {
    fun unlockScenes(state: PetState): Set<PetLcdScene> {
        val unlocked = state.unlockedScenes.toMutableSet()
        unlocked.add(PetLcdScene.COZY)
        PetLcdScene.entries.forEach { scene ->
            if (scene.meetsUnlockThresholds(state)) unlocked.add(scene)
        }
        return unlocked
    }

    fun unlockCosmetics(stats: PetStats, current: Set<PetCosmetic>): Set<PetCosmetic> {
        val unlocked = current.toMutableSet()
        PetCosmetic.entries.forEach { cosmetic ->
            if (stats.skill >= cosmetic.unlockSkill && stats.trust >= cosmetic.unlockTrust) {
                unlocked.add(cosmetic)
            }
        }
        return unlocked
    }

    fun grantNextProp(current: Set<PetLcdProp>): Set<PetLcdProp> {
        val next = PetLcdProp.nextLocked(current) ?: return current
        return current + next
    }
}
