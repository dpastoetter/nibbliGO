package com.nibbli.nibbligo.core.storage.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pet_state")
data class PetStateEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "nibbli",
    val hunger: Int,
    val energy: Int,
    val mood: Int,
    val hygiene: Int = 80,
    val health: Int = 90,
    val discipline: Int = 50,
    val weight: Int = 50,
    val trust: Int,
    val curiosity: Int,
    val skill: Int,
    val expression: String,
    val animation: String = "IDLE",
    val stage: String = "BABY",
    val condition: String = "HEALTHY",
    val activeNeed: String = "NONE",
    val equippedCosmetic: String?,
    val lastInteractionAtMillis: Long,
    val lastTickAtMillis: Long,
    val bornAtMillis: Long,
    val ageMinutes: Long = 0,
    val dialogueLine: String,
    val memorySummary: String = "",
    val unlockedCosmetics: String,
    val hasMess: Boolean = false,
    val roomId: String = "cozy",
    val careScore: Int = 50,
    val criticalNeglectSinceMillis: Long? = null,
)

@Entity(tableName = "model_install")
data class ModelInstallEntity(
    @PrimaryKey val modelId: String,
    val localPath: String,
    val installedAtMillis: Long,
    val sizeBytes: Long,
)

@Entity(tableName = "conversation")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val modelId: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "message",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val content: String,
    val notes: String?,
    val timestampMillis: Long,
    val modelId: String,
)

@Entity(tableName = "saved_prompt")
data class SavedPromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val preset: String?,
    val isFavorite: Boolean,
    val createdAtMillis: Long,
)

@Entity(tableName = "benchmark_run")
data class BenchmarkRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modelId: String,
    val timeToFirstTokenMs: Long,
    val tokensPerSecond: Float,
    val estimatedMemoryMb: Int,
    val thermalNote: String,
    val timestampMillis: Long,
)

@Entity(tableName = "recording")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val durationMs: Long,
    val transcript: String?,
    val summary: String?,
    val createdAtMillis: Long,
)

@Entity(tableName = "skill_install")
data class SkillInstallEntity(
    @PrimaryKey val skillId: String,
    val displayName: String,
    val description: String,
    val localPath: String,
    val version: String,
    val permissions: String,
    val enabled: Boolean,
    val hasJsRuntime: Boolean,
    val installedAtMillis: Long,
)

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: String,
    val status: String,
    val timestampMillis: Long,
    val summary: String,
)
