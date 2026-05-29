package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nibbli.nibbligo.core.domain.repository.BenchmarkRepository
import com.nibbli.nibbligo.core.domain.repository.PromptRepository
import com.nibbli.nibbligo.core.domain.repository.RecordingRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.AudioRecording
import com.nibbli.nibbligo.core.model.BenchmarkRun
import com.nibbli.nibbligo.core.model.AppThemeMode
import com.nibbli.nibbligo.core.model.GenerationParams
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
    val temperature = floatPreferencesKey("temperature")
    val topK = intPreferencesKey("top_k")
    val topP = floatPreferencesKey("top_p")
    val maxTokens = intPreferencesKey("max_tokens")
    val systemPrompt = stringPreferencesKey("system_prompt")
    val allowDownloads = booleanPreferencesKey("allow_downloads")
    val preferredRuntime = stringPreferencesKey("preferred_runtime")
    val petPersonality = stringPreferencesKey("pet_personality")
    val usePetLlm = booleanPreferencesKey("use_pet_llm")
    val petCommentOnAgent = booleanPreferencesKey("pet_comment_on_agent")
    val petMoodPulseMode = stringPreferencesKey("pet_mood_pulse_mode")
    val themeMode = stringPreferencesKey("theme_mode")
    val showDoTab = booleanPreferencesKey("show_do_tab")
  }

  override val defaultModelId: Flow<String?> =
    context.dataStore.data.map { it[Keys.defaultModel] }

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
        PetMoodPulseMode.valueOf(prefs[Keys.petMoodPulseMode] ?: PetMoodPulseMode.NORMAL.name)
      }.getOrDefault(PetMoodPulseMode.NORMAL)
    }

  override val themeMode: Flow<AppThemeMode> =
    context.dataStore.data.map { prefs ->
      runCatching {
        AppThemeMode.valueOf(prefs[Keys.themeMode] ?: AppThemeMode.SYSTEM.name)
      }.getOrDefault(AppThemeMode.SYSTEM)
    }

  override val showDoTab: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.showDoTab] ?: false }

  override suspend fun setDefaultModelId(modelId: String?) {
    context.dataStore.edit { prefs ->
      if (modelId == null) prefs.remove(Keys.defaultModel)
      else prefs[Keys.defaultModel] = modelId
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

  override suspend fun setShowDoTab(show: Boolean) {
    context.dataStore.edit { it[Keys.showDoTab] = show }
  }
}
