package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.core.model.equippedScene
import com.nibbli.nibbligo.core.model.withEquippedScene
import org.json.JSONObject

/** Local-only pet postcard for async friend visits (no server). */
data class PetPostcard(
    val friendCode: String,
    val senderName: String,
    val stage: LifeStage,
    val careScore: Int,
    val dialogueLine: String,
    val mood: Int,
    val equippedCosmetic: String?,
    val roomId: String? = null,
    val equippedProp: String? = null,
    val visitMessage: String? = null,
    val careTip: String? = null,
    val borrowedPropId: String? = null,
    val borrowedSceneId: String? = null,
    val importedAtMillis: Long = System.currentTimeMillis(),
) {
    fun toJson(): String = JSONObject().apply {
        put("v", 2)
        put("friendCode", friendCode)
        put("senderName", senderName)
        put("stage", stage.name)
        put("careScore", careScore)
        put("dialogueLine", dialogueLine)
        put("mood", mood)
        put("equippedCosmetic", equippedCosmetic ?: "")
        put("roomId", roomId ?: "")
        put("equippedProp", equippedProp ?: "")
        put("visitMessage", visitMessage ?: "")
        put("careTip", careTip ?: "")
        put("borrowedPropId", borrowedPropId ?: "")
        put("borrowedSceneId", borrowedSceneId ?: "")
        put("importedAtMillis", importedAtMillis)
    }.toString()

    fun toVisitDisplayState(): PetState {
        val cosmetic = equippedCosmetic?.let { name ->
            runCatching { PetCosmetic.valueOf(name) }.getOrNull()
        }
        val scene = roomId?.let { PetLcdScene.fromId(it) }
        val prop = equippedProp?.let { PetLcdProp.fromId(it) }
        val expression = when {
            mood >= 75 -> PetExpression.HAPPY
            mood >= 50 -> PetExpression.HAPPY
            mood >= 30 -> PetExpression.ATTENTION
            else -> PetExpression.SICK
        }
        val visitAnimation = if (mood >= 50) PetAnimation.PLAY else PetAnimation.IDLE
        return PetState(
            name = senderName,
            stage = stage,
            condition = PetCondition.HEALTHY,
            expression = expression,
            animation = visitAnimation,
            careScore = careScore,
            dialogueLine = dialogueLine,
            stats = PetStats(mood = mood),
            equippedCosmetic = cosmetic,
            unlockedCosmetics = cosmetic?.let { setOf(it) } ?: emptySet(),
            roomId = scene?.id ?: "cozy",
            unlockedScenes = scene?.let { setOf(it) } ?: setOf(PetLcdScene.COZY),
            equippedProp = prop,
            unlockedProps = prop?.let { setOf(it) } ?: emptySet(),
        )
    }

    companion object {
        fun fromPet(state: PetState, visitMessage: String? = null): PetPostcard {
            val tip = careTipFrom(state)
            val propId = state.equippedProp?.id
            val sceneId = state.equippedScene.id
            return PetPostcard(
                friendCode = state.engagement.friendCode.ifBlank {
                    PetEngagementEngine.computeFriendCode(state)
                },
                senderName = state.name,
                stage = state.stage,
                careScore = state.careScore,
                dialogueLine = state.dialogueLine,
                mood = state.stats.mood,
                equippedCosmetic = state.equippedCosmetic?.name,
                roomId = sceneId,
                equippedProp = propId,
                visitMessage = visitMessage?.trim()?.take(120)?.ifBlank { null },
                careTip = tip,
                borrowedPropId = propId,
                borrowedSceneId = sceneId,
            )
        }

        fun careTipFrom(state: PetState): String = when {
            state.stats.hunger < 40 -> "They might need feeding!"
            state.stats.hygiene < 40 -> "Help them stay clean."
            state.stats.energy < 30 -> "They look tired — rest helps."
            state.stats.mood < 40 -> "They need play!"
            else -> "Keep visiting — friends cheer them up!"
        }

        fun fromJson(raw: String): PetPostcard? = runCatching {
            val json = JSONObject(raw)
            when (json.optInt("v", 0)) {
                1 -> PetPostcard(
                    friendCode = json.getString("friendCode"),
                    senderName = json.getString("senderName"),
                    stage = LifeStage.valueOf(json.getString("stage")),
                    careScore = json.getInt("careScore"),
                    dialogueLine = json.getString("dialogueLine"),
                    mood = json.getInt("mood"),
                    equippedCosmetic = json.optString("equippedCosmetic").ifBlank { null },
                    roomId = json.optString("roomId").ifBlank { null },
                    equippedProp = json.optString("equippedProp").ifBlank { null },
                    importedAtMillis = json.optLong("importedAtMillis", System.currentTimeMillis()),
                )
                2 -> PetPostcard(
                    friendCode = json.getString("friendCode"),
                    senderName = json.getString("senderName"),
                    stage = LifeStage.valueOf(json.getString("stage")),
                    careScore = json.getInt("careScore"),
                    dialogueLine = json.getString("dialogueLine"),
                    mood = json.getInt("mood"),
                    equippedCosmetic = json.optString("equippedCosmetic").ifBlank { null },
                    roomId = json.optString("roomId").ifBlank { null },
                    equippedProp = json.optString("equippedProp").ifBlank { null },
                    visitMessage = json.optString("visitMessage").ifBlank { null },
                    careTip = json.optString("careTip").ifBlank { null },
                    borrowedPropId = json.optString("borrowedPropId").ifBlank { null },
                    borrowedSceneId = json.optString("borrowedSceneId").ifBlank { null },
                    importedAtMillis = json.optLong("importedAtMillis", System.currentTimeMillis()),
                )
                else -> return null
            }
        }.getOrNull()

        fun isExpired(postcard: PetPostcard, nowMillis: Long = System.currentTimeMillis()): Boolean =
            nowMillis - postcard.importedAtMillis > VISIT_TTL_MS

        private const val VISIT_TTL_MS = 24 * 60 * 60 * 1000L
    }
}

object PetPostcardCodec {
    fun encode(state: PetState, visitMessage: String? = null): String =
        PetPostcard.fromPet(state, visitMessage).toJson()

    fun decode(raw: String): PetPostcard? = PetPostcard.fromJson(raw.trim())
}

/** Bouncier idle while a friend is visiting on the LCD. */
fun PetState.forVisitPlaydate(): PetState = when {
    condition == PetCondition.DEAD ||
        condition == PetCondition.SLEEPING ||
        animation == PetAnimation.SLEEP ||
        animation == PetAnimation.PLAY ||
        animation == PetAnimation.HAPPY ||
        animation == PetAnimation.EAT ||
        animation == PetAnimation.EVOLVE -> this
    else -> copy(animation = PetAnimation.PLAY)
}

fun PetState.applyBorrowedSouvenir(postcard: PetPostcard): PetState {
    var updated = this
    postcard.borrowedPropId?.let { id ->
        PetLcdProp.fromId(id)?.let { prop ->
            updated = updated.copy(unlockedProps = updated.unlockedProps + prop)
        }
    }
    postcard.borrowedSceneId?.let { id ->
        PetLcdScene.fromId(id)?.let { scene ->
            updated = updated.copy(unlockedScenes = updated.unlockedScenes + scene)
        }
    }
    return updated
}

fun PetState.removeBorrowedSouvenir(postcard: PetPostcard): PetState {
    var updated = this
    postcard.borrowedPropId?.let { id ->
        PetLcdProp.fromId(id)?.let { prop ->
            updated = updated.copy(unlockedProps = updated.unlockedProps - prop)
        }
    }
    postcard.borrowedSceneId?.let { id ->
        PetLcdScene.fromId(id)?.let { scene ->
            updated = updated.copy(unlockedScenes = updated.unlockedScenes - scene)
        }
    }
    return updated
}
