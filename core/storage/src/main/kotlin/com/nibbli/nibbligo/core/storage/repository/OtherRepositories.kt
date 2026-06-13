package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nibbli.nibbligo.core.domain.repository.BenchmarkRepository
import com.nibbli.nibbligo.core.domain.repository.PromptRepository
import com.nibbli.nibbligo.core.domain.repository.RecordingRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.AudioRecording
import com.nibbli.nibbligo.core.model.BenchmarkRun
import com.nibbli.nibbligo.core.model.AppAccentPalette
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.ChatPromptMode
import com.nibbli.nibbligo.core.model.GenerationParams
import com.nibbli.nibbligo.core.model.LiteRtAcceleratorPreference
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.model.SavedPrompt
import com.nibbli.nibbligo.core.storage.local.dao.BenchmarkRunDao
import com.nibbli.nibbligo.core.storage.local.dao.RecordingDao
import com.nibbli.nibbligo.core.storage.local.dao.SavedPromptDao
import com.nibbli.nibbligo.core.storage.local.entity.RecordingEntity
import com.nibbli.nibbligo.core.storage.mapper.toDomain
import com.nibbli.nibbligo.core.storage.mapper.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("nibbli_prefs")

@Singleton
class PromptRepositoryImpl @Inject constructor(
  private val savedPromptDao: SavedPromptDao,
) : PromptRepository {
  override fun observeSavedPrompts(): Flow<List<SavedPrompt>> =
    savedPromptDao.observeAll().map { list -> list.map { it.toDomain() } }

  override suspend fun savePrompt(prompt: SavedPrompt): Long =
    savedPromptDao.insert(prompt.toEntity())

  override suspend fun deletePrompt(id: Long) = savedPromptDao.delete(id)
}

@Singleton
class BenchmarkRepositoryImpl @Inject constructor(
  private val benchmarkRunDao: BenchmarkRunDao,
) : BenchmarkRepository {
  override fun observeRuns(modelId: String?): Flow<List<BenchmarkRun>> =
    if (modelId == null) {
      benchmarkRunDao.observeAll().map { list -> list.map { it.toDomain() } }
    } else {
      benchmarkRunDao.observeForModel(modelId).map { list -> list.map { it.toDomain() } }
    }

  override suspend fun saveRun(run: BenchmarkRun) {
    benchmarkRunDao.insert(run.toEntity())
  }
}

@Singleton
class RecordingRepositoryImpl @Inject constructor(
  private val recordingDao: RecordingDao,
) : RecordingRepository {
  override fun observeRecordings(): Flow<List<AudioRecording>> =
    recordingDao.observeAll().map { list -> list.map { it.toDomain() } }

  override suspend fun saveRecording(recording: AudioRecording): Long =
    recordingDao.insert(
      RecordingEntity(
        uri = recording.uri,
        durationMs = recording.durationMs,
        transcript = recording.transcript,
        summary = recording.summary,
        createdAtMillis = recording.createdAtMillis,
      ),
    )

  override suspend fun updateTranscript(id: Long, transcript: String, summary: String?) {
    recordingDao.updateTranscript(id, transcript, summary)
  }
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
  @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

  private object Keys {
    val defaultModel = stringPreferencesKey("default_model")
    val petModel = stringPreferencesKey("pet_model")
    val temperature = floatPreferencesKey("temperature")
    val topK = intPreferencesKey("top_k")
    val topP = floatPreferencesKey("top_p")
    val maxTokens = intPreferencesKey("max_tokens")
    val systemPrompt = stringPreferencesKey("system_prompt")
    val chatPromptMode = stringPreferencesKey("chat_prompt_mode")
    val allowDownloads = booleanPreferencesKey("allow_downloads")
    val preferredRuntime = stringPreferencesKey("preferred_runtime")
    val petPersonality = stringPreferencesKey("pet_personality")
    val usePetLlm = booleanPreferencesKey("use_pet_llm")
    val petCommentOnAgent = booleanPreferencesKey("pet_comment_on_agent")
    val petMoodPulseMode = stringPreferencesKey("pet_mood_pulse_mode")
    val themeMode = stringPreferencesKey("theme_mode")
    val accentPalette = stringPreferencesKey("accent_palette")
    val showDoTab = booleanPreferencesKey("show_do_tab")
    val litertAccelerator = stringPreferencesKey("litert_accelerator")
    val onboardingCompleted = booleanPreferencesKey("pet_onboarding_completed")
    val caretakerName = stringPreferencesKey("pet_caretaker_name")
    val onboardingAboutYou = stringPreferencesKey("pet_onboarding_about")
    val onboardingGoal = stringPreferencesKey("pet_onboarding_goal")
    val modelSetupPromptDismissed = booleanPreferencesKey("model_setup_prompt_dismissed")
    val termsAccepted = booleanPreferencesKey("terms_accepted")
    val termsAcceptedAt = longPreferencesKey("terms_accepted_at")
    val petSoundHapticsEnabled = booleanPreferencesKey("pet_sound_haptics_enabled")
    val petNotificationsEnabled = booleanPreferencesKey("pet_notifications_enabled")
    val lcdCoachMarksDismissed = booleanPreferencesKey("lcd_coach_marks_dismissed")
    val firstTalkGreetingSent = booleanPreferencesKey("first_talk_greeting_sent")
  }

  override val defaultModelId: Flow<String?> =
    context.dataStore.data.map { it[Keys.defaultModel] }

  override val petModelId: Flow<String?> =
    context.dataStore.data.map { it[Keys.petModel] }

  override val generationParams: Flow<GenerationParams> =
    context.dataStore.data.map { prefs ->
      GenerationParams(
        temperature = prefs[Keys.temperature] ?: 0.7f,
        topK = prefs[Keys.topK] ?: 40,
        topP = prefs[Keys.topP] ?: 0.9f,
        maxTokens = prefs[Keys.maxTokens] ?: 512,
        systemPrompt = prefs[Keys.systemPrompt]
          ?: "You are nibbli, a helpful on-device companion.",
      )
    }

  override val chatPromptMode: Flow<ChatPromptMode> =
    context.dataStore.data.map { prefs ->
      runCatching {
        ChatPromptMode.valueOf(prefs[Keys.chatPromptMode] ?: ChatPromptMode.PIXEL_FRIEND.name)
      }.getOrDefault(ChatPromptMode.PIXEL_FRIEND)
    }

  override val allowDownloads: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.allowDownloads] ?: true }

  override val preferredRuntimeKind: Flow<String> =
    context.dataStore.data.map { it[Keys.preferredRuntime] ?: "litert" }

  override val petPersonality: Flow<PetPersonality> =
    context.dataStore.data.map { prefs ->
      runCatching {
        PetPersonality.valueOf(prefs[Keys.petPersonality] ?: PetPersonality.PLAYFUL.name)
      }.getOrDefault(PetPersonality.PLAYFUL)
    }

  override val usePetLlmReactions: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.usePetLlm] ?: true }

  override val petCommentOnAgentWork: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.petCommentOnAgent] ?: true }

  override val petMoodPulseMode: Flow<PetMoodPulseMode> =
    context.dataStore.data.map { prefs ->
      runCatching {
        PetMoodPulseMode.valueOf(prefs[Keys.petMoodPulseMode] ?: PetMoodPulseMode.QUIET.name)
      }.getOrDefault(PetMoodPulseMode.QUIET)
    }

  override val themeMode: Flow<AppThemeMode> =
    context.dataStore.data.map { prefs ->
      runCatching {
        AppThemeMode.valueOf(prefs[Keys.themeMode] ?: AppThemeMode.SYSTEM.name)
      }.getOrDefault(AppThemeMode.SYSTEM)
    }

  override val accentPalette: Flow<AppAccentPalette> =
    context.dataStore.data.map { prefs ->
      runCatching {
        AppAccentPalette.valueOf(prefs[Keys.accentPalette] ?: AppAccentPalette.TEAL.name)
      }.getOrDefault(AppAccentPalette.TEAL)
    }

  override val showDoTab: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.showDoTab] ?: false }

  override val litertAccelerator: Flow<LiteRtAcceleratorPreference> =
    context.dataStore.data.map { prefs ->
      runCatching {
        LiteRtAcceleratorPreference.valueOf(
          prefs[Keys.litertAccelerator] ?: LiteRtAcceleratorPreference.AUTO.name,
        )
      }.getOrDefault(LiteRtAcceleratorPreference.AUTO)
    }

  override val petOnboardingProfile: Flow<PetOnboardingProfile> =
    context.dataStore.data.map { prefs -> prefs.toOnboardingProfile() }

  override val onboardingCompleted: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.onboardingCompleted] ?: false }

  override val modelSetupPromptDismissed: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.modelSetupPromptDismissed] ?: false }

  override val termsAccepted: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.termsAccepted] ?: false }

  override val petSoundHapticsEnabled: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.petSoundHapticsEnabled] ?: true }

  override val petNotificationsEnabled: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.petNotificationsEnabled] ?: true }

  override val lcdCoachMarksDismissed: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.lcdCoachMarksDismissed] ?: false }

  override val firstTalkGreetingSent: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.firstTalkGreetingSent] ?: false }

  override suspend fun setTermsAccepted(acceptedAtMillis: Long) {
    context.dataStore.edit { prefs ->
      prefs[Keys.termsAccepted] = true
      prefs[Keys.termsAcceptedAt] = acceptedAtMillis
    }
  }

  override suspend fun setDefaultModelId(modelId: String?) {
    context.dataStore.edit { prefs ->
      if (modelId == null) prefs.remove(Keys.defaultModel)
      else prefs[Keys.defaultModel] = modelId
    }
  }

  override suspend fun setPetModelId(modelId: String?) {
    context.dataStore.edit { prefs ->
      if (modelId == null) prefs.remove(Keys.petModel)
      else prefs[Keys.petModel] = modelId
    }
  }

  override suspend fun setGenerationParams(params: GenerationParams) {
    context.dataStore.edit { prefs ->
      prefs[Keys.temperature] = params.temperature
      prefs[Keys.topK] = params.topK
      prefs[Keys.topP] = params.topP
      prefs[Keys.maxTokens] = params.maxTokens
      prefs[Keys.systemPrompt] = params.systemPrompt
    }
  }

  override suspend fun setChatPromptMode(mode: ChatPromptMode) {
    context.dataStore.edit { it[Keys.chatPromptMode] = mode.name }
  }

  override suspend fun setAllowDownloads(allowed: Boolean) {
    context.dataStore.edit { it[Keys.allowDownloads] = allowed }
  }

  override suspend fun setPreferredRuntimeKind(kind: String) {
    context.dataStore.edit { it[Keys.preferredRuntime] = kind }
  }

  override suspend fun setPetPersonality(personality: PetPersonality) {
    context.dataStore.edit { it[Keys.petPersonality] = personality.name }
  }

  override suspend fun setUsePetLlmReactions(enabled: Boolean) {
    context.dataStore.edit { it[Keys.usePetLlm] = enabled }
  }

  override suspend fun setPetCommentOnAgentWork(enabled: Boolean) {
    context.dataStore.edit { it[Keys.petCommentOnAgent] = enabled }
  }

  override suspend fun setPetMoodPulseMode(mode: PetMoodPulseMode) {
    context.dataStore.edit { it[Keys.petMoodPulseMode] = mode.name }
  }

  override suspend fun setThemeMode(mode: AppThemeMode) {
    context.dataStore.edit { it[Keys.themeMode] = mode.name }
  }

  override suspend fun setAccentPalette(palette: AppAccentPalette) {
    context.dataStore.edit { it[Keys.accentPalette] = palette.name }
  }

  override suspend fun setShowDoTab(show: Boolean) {
    context.dataStore.edit { it[Keys.showDoTab] = show }
  }

  override suspend fun setLitertAccelerator(preference: LiteRtAcceleratorPreference) {
    context.dataStore.edit { it[Keys.litertAccelerator] = preference.name }
  }

  override suspend fun setModelSetupPromptDismissed(dismissed: Boolean) {
    context.dataStore.edit { it[Keys.modelSetupPromptDismissed] = dismissed }
  }

  override suspend fun setPetOnboardingProfile(profile: PetOnboardingProfile) {
    context.dataStore.edit { prefs ->
      prefs[Keys.onboardingCompleted] = profile.completed
      if (profile.caretakerName.isBlank()) prefs.remove(Keys.caretakerName)
      else prefs[Keys.caretakerName] = profile.caretakerName.trim()
      if (profile.aboutYou.isBlank()) prefs.remove(Keys.onboardingAboutYou)
      else prefs[Keys.onboardingAboutYou] = profile.aboutYou.trim()
      if (profile.companionGoal.isBlank()) prefs.remove(Keys.onboardingGoal)
      else prefs[Keys.onboardingGoal] = profile.companionGoal.trim()
    }
  }

  override suspend fun setPetSoundHapticsEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.petSoundHapticsEnabled] = enabled }
  }

  override suspend fun setPetNotificationsEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.petNotificationsEnabled] = enabled }
  }

  override suspend fun setLcdCoachMarksDismissed(dismissed: Boolean) {
    context.dataStore.edit { it[Keys.lcdCoachMarksDismissed] = dismissed }
  }

  override suspend fun setFirstTalkGreetingSent(sent: Boolean) {
    context.dataStore.edit { it[Keys.firstTalkGreetingSent] = sent }
  }

  override suspend fun getPetNotificationsEnabled(): Boolean =
    context.dataStore.data.map { it[Keys.petNotificationsEnabled] ?: true }.first()

  private fun Preferences.toOnboardingProfile(): PetOnboardingProfile =
    PetOnboardingProfile(
      caretakerName = this[Keys.caretakerName].orEmpty(),
      aboutYou = this[Keys.onboardingAboutYou].orEmpty(),
      companionGoal = this[Keys.onboardingGoal].orEmpty(),
      completed = this[Keys.onboardingCompleted] ?: false,
    )
}
