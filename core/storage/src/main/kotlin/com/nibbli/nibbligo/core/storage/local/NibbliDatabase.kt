package com.nibbli.nibbligo.core.storage.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nibbli.nibbligo.core.storage.local.dao.ActionHistoryDao
import com.nibbli.nibbligo.core.storage.local.dao.BenchmarkRunDao
import com.nibbli.nibbligo.core.storage.local.dao.CompanionMemoryFactDao
import com.nibbli.nibbligo.core.storage.local.dao.ConversationDao
import com.nibbli.nibbligo.core.storage.local.dao.MessageDao
import com.nibbli.nibbligo.core.storage.local.dao.ModelInstallDao
import com.nibbli.nibbligo.core.storage.local.dao.PetStateDao
import com.nibbli.nibbligo.core.storage.local.dao.RecordingDao
import com.nibbli.nibbligo.core.storage.local.dao.SavedPromptDao
import com.nibbli.nibbligo.core.storage.local.dao.SkillInstallDao
import com.nibbli.nibbligo.core.storage.local.entity.ActionHistoryEntity
import com.nibbli.nibbligo.core.storage.local.entity.BenchmarkRunEntity
import com.nibbli.nibbligo.core.storage.local.entity.CompanionMemoryFactEntity
import com.nibbli.nibbligo.core.storage.local.entity.ConversationEntity
import com.nibbli.nibbligo.core.storage.local.entity.MessageEntity
import com.nibbli.nibbligo.core.storage.local.entity.ModelInstallEntity
import com.nibbli.nibbligo.core.storage.local.entity.PetStateEntity
import com.nibbli.nibbligo.core.storage.local.entity.RecordingEntity
import com.nibbli.nibbligo.core.storage.local.entity.SavedPromptEntity
import com.nibbli.nibbligo.core.storage.local.entity.SkillInstallEntity

@Database(
    entities = [
        PetStateEntity::class,
        CompanionMemoryFactEntity::class,
        ModelInstallEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        SavedPromptEntity::class,
        BenchmarkRunEntity::class,
        RecordingEntity::class,
        ActionHistoryEntity::class,
        SkillInstallEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class NibbliDatabase : RoomDatabase() {
    abstract fun petStateDao(): PetStateDao
    abstract fun companionMemoryFactDao(): CompanionMemoryFactDao
    abstract fun modelInstallDao(): ModelInstallDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun savedPromptDao(): SavedPromptDao
    abstract fun benchmarkRunDao(): BenchmarkRunDao
    abstract fun recordingDao(): RecordingDao
    abstract fun actionHistoryDao(): ActionHistoryDao
    abstract fun skillInstallDao(): SkillInstallDao
}
