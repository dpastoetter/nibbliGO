package com.nibbli.nibbligo.core.domain.repository

import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.AudioRecording
import com.nibbli.nibbligo.core.model.BenchmarkRun
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.model.SavedPrompt
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun observeCatalog(): Flow<List<ModelInfo>>
    fun observeInstalled(): Flow<List<InstalledModel>>
    suspend fun getCatalog(): List<ModelInfo>
    suspend fun getInstalled(): List<InstalledModel>
    suspend fun isInstalled(modelId: String): Boolean
    suspend fun install(modelId: String): Result<Unit>
    suspend fun uninstall(modelId: String): Result<Unit>
    suspend fun getInstalledModelIds(): List<String>
}

interface PetRepository {
    fun observePetState(): Flow<PetState>
    suspend fun getPetState(): PetState
    suspend fun savePetState(state: PetState)
}

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>
    suspend fun createConversation(modelId: String, title: String): Long
    suspend fun saveMessage(message: ChatMessage)
    suspend fun updateConversation(conversation: Conversation)
    suspend fun deleteAllConversations()
}

interface PromptRepository {
    fun observeSavedPrompts(): Flow<List<SavedPrompt>>
    suspend fun savePrompt(prompt: SavedPrompt): Long
    suspend fun deletePrompt(id: Long)
}

interface BenchmarkRepository {
    fun observeRuns(modelId: String? = null): Flow<List<BenchmarkRun>>
    suspend fun saveRun(run: BenchmarkRun)
}

interface RecordingRepository {
    fun observeRecordings(): Flow<List<AudioRecording>>
    suspend fun saveRecording(recording: AudioRecording): Long
    suspend fun updateTranscript(id: Long, transcript: String, summary: String?)
}

interface UserPreferencesRepository {
    val defaultModelId: Flow<String?>
    val generationParams: Flow<GenerationParams>
    val allowDownloads: Flow<Boolean>
    val preferredRuntimeKind: Flow<String>
    val petPersonality: Flow<PetPersonality>
    val usePetLlmReactions: Flow<Boolean>
    val themeMode: Flow<AppThemeMode>
    val showDoTab: Flow<Boolean>
    suspend fun setDefaultModelId(modelId: String?)
    suspend fun setGenerationParams(params: GenerationParams)
    suspend fun setAllowDownloads(allowed: Boolean)
    suspend fun setPreferredRuntimeKind(kind: String)
    suspend fun setPetPersonality(personality: PetPersonality)
    suspend fun setUsePetLlmReactions(enabled: Boolean)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setShowDoTab(show: Boolean)
}
