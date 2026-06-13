package com.nibbli.nibbligo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.CompanionMemoryFact
import com.nibbli.nibbligo.core.model.CompanionMemoryFactSource
import com.nibbli.nibbligo.core.model.InstalledModel
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.pet.llm.CompanionMemoryStore
import com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader
import com.nibbli.nibbligo.core.pet.llm.PetModelResolver
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanionUiState(
    val installedModelIds: List<String> = emptyList(),
    val petModelId: String? = null,
    val petPersonality: PetPersonality = PetPersonality.PLAYFUL,
    val petCommentOnAgentWork: Boolean = true,
    val petMoodPulseMode: PetMoodPulseMode = PetMoodPulseMode.NORMAL,
    val petSoundHapticsEnabled: Boolean = true,
    val petName: String = "nibbli",
    val caretakerName: String = "",
    val aboutYou: String = "",
    val companionGoal: String = "",
    val memoryFacts: List<CompanionMemoryFact> = emptyList(),
    val newFactDraft: String = "",
    val companionSaveMessage: String? = null,
    val memorySaveMessage: String? = null,
)

@HiltViewModel
class CompanionViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val modelRepository: ModelRepository,
    private val petRepository: PetRepository,
    private val companionMemoryStore: CompanionMemoryStore,
    private val liteRtModelPreloader: LiteRtModelPreloader,
    private val inferenceRuntime: InferenceRuntime,
    private val petModelResolver: PetModelResolver,
    private val modelAvailabilityGate: ModelAvailabilityGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { companionMemoryStore.ensureMigrated() }
        viewModelScope.launch {
            combine(
                modelRepository.observeInstalled(),
                userPreferencesRepository.petModelId,
                userPreferencesRepository.petPersonality,
                userPreferencesRepository.petCommentOnAgentWork,
                userPreferencesRepository.petMoodPulseMode,
                userPreferencesRepository.petSoundHapticsEnabled,
                userPreferencesRepository.petOnboardingProfile,
                petRepository.observePetState(),
                companionMemoryStore.observeFacts(),
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val installed = values[0] as List<InstalledModel>
                val petModelId = values[1] as String?
                val personality = values[2] as PetPersonality
                val commentOnAgent = values[3] as Boolean
                val moodPulse = values[4] as PetMoodPulseMode
                val soundHaptics = values[5] as Boolean
                val profile = values[6] as PetOnboardingProfile
                val pet = values[7] as com.nibbli.nibbligo.core.model.PetState
                val facts = values[8] as List<CompanionMemoryFact>
                CompanionUiState(
                    installedModelIds = installed.map { it.modelId },
                    petModelId = petModelId,
                    petPersonality = personality,
                    petCommentOnAgentWork = commentOnAgent,
                    petMoodPulseMode = moodPulse,
                    petSoundHapticsEnabled = soundHaptics,
                    petName = pet.name.ifBlank { "nibbli" },
                    caretakerName = profile.caretakerName,
                    aboutYou = profile.aboutYou,
                    companionGoal = profile.companionGoal,
                    memoryFacts = facts,
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(
                        newFactDraft = current.newFactDraft,
                        companionSaveMessage = current.companionSaveMessage,
                        memorySaveMessage = current.memorySaveMessage,
                    )
                }
            }
        }
    }

    fun onPetNameChange(value: String) {
        _uiState.update { it.copy(petName = value) }
    }

    fun onCaretakerNameChange(value: String) {
        _uiState.update { it.copy(caretakerName = value) }
    }

    fun onAboutYouChange(value: String) {
        _uiState.update { it.copy(aboutYou = value) }
    }

    fun onCompanionGoalChange(value: String) {
        _uiState.update { it.copy(companionGoal = value) }
    }

    fun onNewFactDraftChange(value: String) {
        _uiState.update { it.copy(newFactDraft = value) }
    }

    fun addMemoryFact() {
        val draft = _uiState.value.newFactDraft.trim()
        if (draft.isBlank()) return
        viewModelScope.launch {
            companionMemoryStore.addFact(draft, CompanionMemoryFactSource.MANUAL)
            _uiState.update {
                it.copy(
                    newFactDraft = "",
                    memorySaveMessage = "Memory saved",
                )
            }
        }
    }

    fun removeMemoryFact(id: String) {
        viewModelScope.launch {
            companionMemoryStore.removeFact(id)
            _uiState.update { it.copy(memorySaveMessage = "Memory updated") }
        }
    }

    fun saveCompanionProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            val profile = PetOnboardingProfile(
                caretakerName = state.caretakerName.trim(),
                aboutYou = state.aboutYou.trim(),
                companionGoal = state.companionGoal.trim(),
                completed = true,
            )
            userPreferencesRepository.setPetOnboardingProfile(profile)
            val pet = petRepository.getPetState()
            val trimmedName = state.petName.trim().ifBlank { "nibbli" }
            if (pet.name != trimmedName) {
                petRepository.savePetState(pet.copy(name = trimmedName))
            }
            liteRtModelPreloader.invalidate()
            runCatching {
                if (modelAvailabilityGate.hasUsableModel()) {
                    inferenceRuntime.resetHomeTalkSession(petModelResolver.resolve())
                }
            }
            liteRtModelPreloader.preloadPrimaryModel(force = true)
            _uiState.update { it.copy(companionSaveMessage = "Companion profile saved") }
        }
    }

    fun clearAllMemory() {
        viewModelScope.launch {
            companionMemoryStore.clearAll()
            _uiState.update {
                it.copy(
                    newFactDraft = "",
                    memorySaveMessage = "Memory cleared",
                )
            }
        }
    }

    fun clearCompanionSaveMessage() {
        _uiState.update { it.copy(companionSaveMessage = null) }
    }

    fun clearMemorySaveMessage() {
        _uiState.update { it.copy(memorySaveMessage = null) }
    }

    fun setPetModelId(modelId: String?) {
        viewModelScope.launch { userPreferencesRepository.setPetModelId(modelId) }
    }

    fun setPetPersonality(personality: PetPersonality) {
        viewModelScope.launch { userPreferencesRepository.setPetPersonality(personality) }
    }

    fun setPetCommentOnAgentWork(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPetCommentOnAgentWork(enabled) }
    }

    fun setPetMoodPulseMode(mode: PetMoodPulseMode) {
        viewModelScope.launch { userPreferencesRepository.setPetMoodPulseMode(mode) }
    }

    fun setPetSoundHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setPetSoundHapticsEnabled(enabled) }
    }
}
