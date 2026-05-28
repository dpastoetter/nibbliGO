package com.nibbli.nibbligo.core.storage.mapper

import com.nibbli.nibbligo.core.model.AudioRecording
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.BenchmarkRun
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.core.model.PromptPreset
import com.nibbli.nibbligo.core.model.SavedPrompt
import com.nibbli.nibbligo.core.storage.local.entity.BenchmarkRunEntity
import com.nibbli.nibbligo.core.storage.local.entity.ConversationEntity
import com.nibbli.nibbligo.core.storage.local.entity.MessageEntity
import com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity
import com.nibbli.nibbligo.core.storage.local.entity.PetStateEntity
import com.nibbli.nibbligo.core.storage.local.entity.RecordingEntity
import com.nibbli.nibbligo.core.storage.local.entity.SavedPromptEntity

fun PetStateEntity.toDomain(): PetState = PetState(
    stats = PetStats(hunger, energy, mood, trust, curiosity, skill),
    expression = PetExpression.valueOf(expression),
    equippedCosmetic = equippedCosmetic?.let { PetCosmetic.valueOf(it) },
    lastInteractionAtMillis = lastInteractionAtMillis,
    dialogueLine = dialogueLine,
    unlockedCosmetics = unlockedCosmetics.split(",")
        .filter { it.isNotBlank() }
        .map { PetCosmetic.valueOf(it) }
        .toSet(),
)

fun PetState.toEntity(): PetStateEntity = PetStateEntity(
    id = 1,
    hunger = stats.hunger,
    energy = stats.energy,
    mood = stats.mood,
    trust = stats.trust,
    curiosity = stats.curiosity,
    skill = stats.skill,
    expression = expression.name,
    equippedCosmetic = equippedCosmetic?.name,
    lastInteractionAtMillis = lastInteractionAtMillis,
    dialogueLine = dialogueLine,
    unlockedCosmetics = unlockedCosmetics.joinToString(",") { it.name },
)

fun ModelInstallEntity.toDomain() = InstalledModel(modelId, localPath, installedAtMillis, sizeBytes)

fun ConversationEntity.toDomain() = Conversation(id, title, modelId, createdAtMillis, updatedAtMillis)

fun Conversation.toEntity() = ConversationEntity(id, title, modelId, createdAtMillis, updatedAtMillis)

fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    conversationId = conversationId,
    role = MessageRole.valueOf(role),
    content = content,
    notes = notes,
    timestampMillis = timestampMillis,
    modelId = modelId,
)

fun ChatMessage.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    notes = notes,
    timestampMillis = timestampMillis,
    modelId = modelId,
)

fun SavedPromptEntity.toDomain() = SavedPrompt(
    id = id,
    title = title,
    body = body,
    preset = preset?.let { PromptPreset.valueOf(it) },
    isFavorite = isFavorite,
    createdAtMillis = createdAtMillis,
)

fun SavedPrompt.toEntity() = SavedPromptEntity(
    id = id,
    title = title,
    body = body,
    preset = preset?.name,
    isFavorite = isFavorite,
    createdAtMillis = createdAtMillis,
)

fun BenchmarkRunEntity.toDomain() = BenchmarkRun(
    id = id,
    modelId = modelId,
    metrics = BenchmarkMetrics(timeToFirstTokenMs, tokensPerSecond, estimatedMemoryMb, thermalNote),
    timestampMillis = timestampMillis,
)

fun BenchmarkRun.toEntity() = BenchmarkRunEntity(
    id = id,
    modelId = modelId,
    timeToFirstTokenMs = metrics.timeToFirstTokenMs,
    tokensPerSecond = metrics.tokensPerSecond,
    estimatedMemoryMb = metrics.estimatedMemoryMb,
    thermalNote = metrics.thermalNote,
    timestampMillis = timestampMillis,
)

fun RecordingEntity.toDomain() = AudioRecording(id, uri, durationMs, transcript, summary, createdAtMillis)
