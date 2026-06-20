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

interface CompanionMemoryRepository {
    fun observeFacts(): Flow<List<com.nibbli.nibbligo.core.model.CompanionMemoryFact>>
    suspend fun getFacts(): List<com.nibbli.nibbligo.core.model.CompanionMemoryFact>
    suspend fun addFact(text: String, source: com.nibbli.nibbligo.core.model.CompanionMemoryFactSource): com.nibbli.nibbligo.core.model.CompanionMemoryFact
    suspend fun updateFact(id: String, text: String)
    suspend fun removeFact(id: String)
    suspend fun clearAll()
    suspend fun replaceAll(facts: List<com.nibbli.nibbligo.core.model.CompanionMemoryFact>)
}

interface ChatRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>>
    suspend fun createConversation(modelId: String, title: String): Long
    suspend fun findConversationByTitle(title: String): Conversation?
    suspend fun getOrCreateConversation(title: String, modelId: String): Long
    suspend fun getRecentMessagesForTitle(title: String, modelId: String, limit: Int): List<ChatMessage>
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

/**
 * Parent/guardian controls. Stored separately from [UserPreferencesRepository] so the
 * many existing test fakes of that interface remain untouched.
 */
interface ParentalControlsRepository {
    /** Salted SHA-256 hex of the parent PIN, or null when no PIN has been set. */
    val pinHash: Flow<String?>

    /** When true, Agent, Benchmark, Prompt Lab, and the HF token panel require the parent PIN. */
    val restrictAdultFeatures: Flow<Boolean>

    suspend fun setPin(rawPin: String?)
    suspend fun verifyPin(rawPin: String): Boolean
    suspend fun isPinSet(): Boolean
    suspend fun setRestrictAdultFeatures(enabled: Boolean)
}

/** Accessibility preferences kept apart from the broad [UserPreferencesRepository]. */
interface AccessibilityPreferencesRepository {
    /** Text size multiplier applied on top of the system font scale. 1.0 = default. */
    val fontScale: Flow<Float>
    suspend fun setFontScale(scale: Float)
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
    val petSoundHapticsEnabled: Flow<Boolean>
    val petNotificationsEnabled: Flow<Boolean>
    val lcdCoachMarksDismissed: Flow<Boolean>
    val firstTalkGreetingSent: Flow<Boolean>
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
    suspend fun setPetSoundHapticsEnabled(enabled: Boolean)
    suspend fun setPetNotificationsEnabled(enabled: Boolean)
    suspend fun setLcdCoachMarksDismissed(dismissed: Boolean)
    suspend fun setFirstTalkGreetingSent(sent: Boolean)
    suspend fun getPetNotificationsEnabled(): Boolean
}
