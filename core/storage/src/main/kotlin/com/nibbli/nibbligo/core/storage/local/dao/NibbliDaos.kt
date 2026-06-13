package com.nibbli.nibbligo.core.storage.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionMemoryFactDao {
    @Query("SELECT * FROM companion_memory_fact ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<CompanionMemoryFactEntity>>

    @Query("SELECT * FROM companion_memory_fact ORDER BY createdAtMillis ASC")
    suspend fun getAll(): List<CompanionMemoryFactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CompanionMemoryFactEntity)

    @Query("DELETE FROM companion_memory_fact WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM companion_memory_fact")
    suspend fun deleteAll()
}

@Dao
interface PetStateDao {
    @Query("SELECT * FROM pet_state WHERE id = 1")
    fun observe(): Flow<PetStateEntity?>

    @Query("SELECT * FROM pet_state WHERE id = 1")
    suspend fun get(): PetStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PetStateEntity)
}

@Dao
interface ModelInstallDao {
    @Query("SELECT * FROM model_install")
    fun observeAll(): Flow<List<ModelInstallEntity>>

    @Query("SELECT * FROM model_install")
    suspend fun getAll(): List<ModelInstallEntity>

    @Query("SELECT * FROM model_install WHERE modelId = :modelId")
    suspend fun get(modelId: String): ModelInstallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ModelInstallEntity)

    @Query("DELETE FROM model_install WHERE modelId = :modelId")
    suspend fun delete(modelId: String)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConversationEntity): Long

    @Update
    suspend fun update(entity: ConversationEntity)

    @Query("DELETE FROM conversation")
    suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY timestampMillis ASC")
    fun observe(conversationId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MessageEntity): Long

    @Query("SELECT * FROM message WHERE conversationId = :conversationId ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getRecent(conversationId: Long, limit: Int): List<MessageEntity>

    @Query("DELETE FROM message WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Long)
}

@Dao
interface SavedPromptDao {
    @Query("SELECT * FROM saved_prompt ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<SavedPromptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedPromptEntity): Long

    @Query("DELETE FROM saved_prompt WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface BenchmarkRunDao {
    @Query("SELECT * FROM benchmark_run ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<BenchmarkRunEntity>>

    @Query("SELECT * FROM benchmark_run WHERE modelId = :modelId ORDER BY timestampMillis DESC")
    fun observeForModel(modelId: String): Flow<List<BenchmarkRunEntity>>

    @Insert
    suspend fun insert(entity: BenchmarkRunEntity): Long
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recording ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Insert
    suspend fun insert(entity: RecordingEntity): Long

    @Query("UPDATE recording SET transcript = :transcript, summary = :summary WHERE id = :id")
    suspend fun updateTranscript(id: Long, transcript: String, summary: String?)
}

@Dao
interface ActionHistoryDao {
    @Insert
    suspend fun insert(entity: ActionHistoryEntity): Long

    @Query("SELECT * FROM action_history ORDER BY timestampMillis DESC LIMIT 50")
    fun observeRecent(): Flow<List<ActionHistoryEntity>>
}

@Dao
interface SkillInstallDao {
    @Query("SELECT * FROM skill_install ORDER BY displayName ASC")
    fun observeAll(): Flow<List<SkillInstallEntity>>

    @Query("SELECT * FROM skill_install WHERE skillId = :skillId")
    suspend fun get(skillId: String): SkillInstallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SkillInstallEntity)

    @Query("UPDATE skill_install SET enabled = :enabled WHERE skillId = :skillId")
    suspend fun setEnabled(skillId: String, enabled: Boolean)

    @Query("DELETE FROM skill_install WHERE skillId = :skillId")
    suspend fun delete(skillId: String)
}
