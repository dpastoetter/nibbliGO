package com.nibbli.nibbligo.core.storage.mapper

import com.nibbli.nibbligo.core.model.CompanionMemoryFact
import com.nibbli.nibbligo.core.model.CompanionMemoryFactSource
import com.nibbli.nibbligo.core.model.AudioRecording
import com.nibbli.nibbligo.core.model.BenchmarkMetrics
import com.nibbli.nibbligo.core.model.BenchmarkRun
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.MessageRole
import com.nibbli.nibbligo.core.model.PetAnimation
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEngagement
import com.nibbli.nibbligo.core.model.PetExpression
import com.nibbli.nibbligo.core.model.PetLcdProp
import com.nibbli.nibbligo.core.model.PetLcdScene
import com.nibbli.nibbligo.core.model.PetNeed
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.PetStats
import com.nibbli.nibbligo.core.model.PromptPreset
import com.nibbli.nibbligo.core.model.SavedPrompt
import com.nibbli.nibbligo.core.storage.local.entity.CompanionMemoryFactEntity
import com.nibbli.nibbligo.core.storage.local.entity.BenchmarkRunEntity
import com.nibbli.nibbligo.core.storage.local.entity.ConversationEntity
import com.nibbli.nibbligo.core.storage.local.entity.MessageEntity
import com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity
import com.nibbli.nibbligo.core.storage.local.entity.PetStateEntity
import com.nibbli.nibbligo.core.storage.local.entity.RecordingEntity
import com.nibbli.nibbligo.core.storage.local.entity.SavedPromptEntity

fun PetStateEntity.toDomain(): PetState = PetState(
    name = name,
    stats = PetStats(
        hunger = hunger,
        energy = energy,
        mood = mood,
        hygiene = hygiene,
        health = health,
        discipline = discipline,
        weight = weight,
        trust = trust,
        curiosity = curiosity,
        skill = skill,
    ),
    stage = runCatching { LifeStage.valueOf(stage) }.getOrDefault(LifeStage.BABY),
    condition = runCatching { PetCondition.valueOf(condition) }.getOrDefault(PetCondition.HEALTHY),
    activeNeed = runCatching { PetNeed.valueOf(activeNeed) }.getOrDefault(PetNeed.NONE),
    expression = runCatching { PetExpression.valueOf(expression) }.getOrDefault(PetExpression.NEUTRAL),
    animation = runCatching { PetAnimation.valueOf(animation) }.getOrDefault(PetAnimation.IDLE),
    equippedCosmetic = equippedCosmetic?.let { PetCosmetic.valueOf(it) },
    lastInteractionAtMillis = lastInteractionAtMillis,
    lastTickAtMillis = lastTickAtMillis,
    bornAtMillis = bornAtMillis,
    ageMinutes = ageMinutes,
    dialogueLine = dialogueLine,
    memorySummary = memorySummary,
    unlockedCosmetics = unlockedCosmetics.split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { runCatching { PetCosmetic.valueOf(it) }.getOrNull() }
        .toSet(),
    unlockedScenes = unlockedScenes.split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { PetLcdScene.fromId(it) }
        .toSet()
        .ifEmpty { setOf(PetLcdScene.COZY) },
    unlockedProps = unlockedProps.split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { PetLcdProp.fromId(it) }
        .toSet(),
    equippedProp = equippedProp?.let { PetLcdProp.fromId(it) },
    hasMess = hasMess,
    roomId = roomId,
    careScore = careScore,
    criticalNeglectSinceMillis = criticalNeglectSinceMillis,
    engagement = PetEngagement(
        careStreakDays = careStreakDays,
        lastCareDayEpoch = lastCareDayEpoch,
        dailyQuestDayEpoch = dailyQuestDayEpoch,
        dailyQuestFeed = dailyQuestFeed,
        dailyQuestPlay = dailyQuestPlay,
        dailyQuestTalk = dailyQuestTalk,
        dailyQuestBonusClaimed = dailyQuestBonusClaimed,
        catchHighScore = catchHighScore,
        catchBestCombo = catchBestCombo,
        dailyCatchTargetScore = dailyCatchTargetScore,
        dailyCatchDayEpoch = dailyCatchDayEpoch,
        dailyCatchChallengeCompleted = dailyCatchChallengeCompleted,
        friendCode = friendCode,
    ),
)

fun PetState.toEntity(): PetStateEntity = PetStateEntity(
    id = 1,
    name = name,
    hunger = stats.hunger,
    energy = stats.energy,
    mood = stats.mood,
    hygiene = stats.hygiene,
    health = stats.health,
    discipline = stats.discipline,
    weight = stats.weight,
    trust = stats.trust,
    curiosity = stats.curiosity,
    skill = stats.skill,
    expression = expression.name,
    animation = animation.name,
    stage = stage.name,
    condition = condition.name,
    activeNeed = activeNeed.name,
    equippedCosmetic = equippedCosmetic?.name,
    lastInteractionAtMillis = lastInteractionAtMillis,
    lastTickAtMillis = lastTickAtMillis,
    bornAtMillis = bornAtMillis,
    ageMinutes = ageMinutes,
    dialogueLine = dialogueLine,
    memorySummary = memorySummary,
    unlockedCosmetics = unlockedCosmetics.joinToString(",") { it.name },
    unlockedScenes = unlockedScenes.joinToString(",") { it.id },
    unlockedProps = unlockedProps.joinToString(",") { it.id },
    equippedProp = equippedProp?.id,
    hasMess = hasMess,
    roomId = roomId,
    careScore = careScore,
    criticalNeglectSinceMillis = criticalNeglectSinceMillis,
    careStreakDays = engagement.careStreakDays,
    lastCareDayEpoch = engagement.lastCareDayEpoch,
    dailyQuestDayEpoch = engagement.dailyQuestDayEpoch,
    dailyQuestFeed = engagement.dailyQuestFeed,
    dailyQuestPlay = engagement.dailyQuestPlay,
    dailyQuestTalk = engagement.dailyQuestTalk,
    dailyQuestBonusClaimed = engagement.dailyQuestBonusClaimed,
    catchHighScore = engagement.catchHighScore,
    catchBestCombo = engagement.catchBestCombo,
    dailyCatchTargetScore = engagement.dailyCatchTargetScore,
    dailyCatchDayEpoch = engagement.dailyCatchDayEpoch,
    dailyCatchChallengeCompleted = engagement.dailyCatchChallengeCompleted,
    friendCode = engagement.friendCode,
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

fun CompanionMemoryFactEntity.toDomain() = CompanionMemoryFact(
    id = id,
    text = text,
    source = runCatching { CompanionMemoryFactSource.valueOf(source) }
        .getOrDefault(CompanionMemoryFactSource.MANUAL),
    createdAtMillis = createdAtMillis,
)

fun CompanionMemoryFact.toEntity() = CompanionMemoryFactEntity(
    id = id,
    text = text,
    source = source.name,
    createdAtMillis = createdAtMillis,
)

fun RecordingEntity.toDomain() = AudioRecording(id, uri, durationMs, transcript, summary, createdAtMillis)
