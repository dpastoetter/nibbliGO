package com.nibbli.nibbligo.feature.pet.domain

import com.nibbli.nibbligo.core.model.LifeStage
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
    val importedAtMillis: Long = System.currentTimeMillis(),
) {
    fun toJson(): String = JSONObject().apply {
        put("v", 1)
        put("friendCode", friendCode)
        put("senderName", senderName)
        put("stage", stage.name)
        put("careScore", careScore)
        put("dialogueLine", dialogueLine)
        put("mood", mood)
        put("equippedCosmetic", equippedCosmetic ?: "")
        put("roomId", roomId ?: "")
        put("equippedProp", equippedProp ?: "")
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
            mood >= 50 -> PetExpression.NEUTRAL
            mood >= 30 -> PetExpression.ATTENTION
            else -> PetExpression.SICK
        }
        return PetState(
            name = senderName,
            stage = stage,
            condition = PetCondition.HEALTHY,
            expression = expression,
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
        fun fromPet(state: PetState): PetPostcard = PetPostcard(
            friendCode = state.engagement.friendCode.ifBlank {
                PetEngagementEngine.computeFriendCode(state)
            },
            senderName = state.name,
            stage = state.stage,
            careScore = state.careScore,
            dialogueLine = state.dialogueLine,
            mood = state.stats.mood,
            equippedCosmetic = state.equippedCosmetic?.name,
            roomId = state.equippedScene.id,
            equippedProp = state.equippedProp?.id,
        )

        fun fromJson(raw: String): PetPostcard? = runCatching {
            val json = JSONObject(raw)
            if (json.optInt("v", 0) != 1) return null
            PetPostcard(
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
        }.getOrNull()

        fun isExpired(postcard: PetPostcard, nowMillis: Long = System.currentTimeMillis()): Boolean =
            nowMillis - postcard.importedAtMillis > VISIT_TTL_MS

        private const val VISIT_TTL_MS = 24 * 60 * 60 * 1000L
    }
}

object PetPostcardCodec {
    fun encode(state: PetState): String = PetPostcard.fromPet(state).toJson()

    fun decode(raw: String): PetPostcard? = PetPostcard.fromJson(raw.trim())
}
