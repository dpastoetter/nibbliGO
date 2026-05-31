package com.nibbli.nibbligo.core.domain.repository

import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.AudioRecording
import com.nibbli.nibbligo.core.model.BenchmarkRun
import com.nibbli.nibbligo.core.model.ChatMessage
import com.nibbli.nibbligo.core.model.Conversation
import com.nibbli.nibbligo.core.model.ChatPromptMode
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.model.ModelInfo
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
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
    suspend fun findConversationByTitle(title: String): Conversation?
    suspend fun getOrCreateConversation(title: String, modelId: String): Long
    suspend fun saveMessage(message: ChatMessage)
    suspend fun deleteMessagesForConversation(conversationId: Long)
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
    val petModelId: Flow<String?>
    val generationParams: Flow<GenerationParams>
    val chatPromptMode: Flow<ChatPromptMode>
    val allowDownloads: Flow<Boolean>
    val preferredRuntimeKind: Flow<String>
    val petPersonality: Flow<PetPersonality>
    val usePetLlmReactions: Flow<Boolean>
    val petCommentOnAgentWork: Flow<Boolean>
    val petMoodPulseMode: Flow<PetMoodPulseMode>
    val themeMode: Flow<AppThemeMode>
    val accentPalette: Flow<AppAccentPalette>
    val showDoTab: Flow<Boolean>
    val litertAccelerator: Flow<LiteRtAcceleratorPreference>
    val petOnboardingProfile: Flow<PetOnboardingProfile>
    val onboardingCompleted: Flow<Boolean>
    val termsAccepted: Flow<Boolean>
    val modelSetupPromptDismissed: Flow<Boolean>
    suspend fun setDefaultModelId(modelId: String?)
    suspend fun setPetModelId(modelId: String?)
    suspend fun setGenerationParams(params: GenerationParams)
    suspend fun setChatPromptMode(mode: ChatPromptMode)
    suspend fun setAllowDownloads(allowed: Boolean)
    suspend fun setPreferredRuntimeKind(kind: String)
    suspend fun setPetPersonality(personality: PetPersonality)
    suspend fun setUsePetLlmReactions(enabled: Boolean)
    suspend fun setPetCommentOnAgentWork(enabled: Boolean)
    suspend fun setPetMoodPulseMode(mode: PetMoodPulseMode)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setAccentPalette(palette: AppAccentPalette)
    suspend fun setShowDoTab(show: Boolean)
    suspend fun setLitertAccelerator(preference: LiteRtAcceleratorPreference)
    suspend fun setPetOnboardingProfile(profile: PetOnboardingProfile)
    suspend fun setTermsAccepted(acceptedAtMillis: Long)
    suspend fun setModelSetupPromptDismissed(dismissed: Boolean)
}
