package com.nibbli.nibbligo.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.PetOnboardingProfile
import com.nibbli.nibbligo.core.model.PetPersonality
import com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader
import com.nibbli.nibbligo.core.pet.llm.PetModelResolver
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PetOnboardingUiState(
    val stepIndex: Int = 0,
    val caretakerName: String = "",
    val petName: String = "nibbli",
    val personality: PetPersonality = PetPersonality.PLAYFUL,
    val aboutYou: String = "",
    val companionGoal: String = "",
    val termsAccepted: Boolean = false,
    val isSaving: Boolean = false,
) {
    val stepCount: Int = 6

    val canContinue: Boolean
        get() = when (stepIndex) {
            0 -> true
            1 -> petName.trim().isNotBlank()
            2 -> caretakerName.trim().isNotBlank()
            3, 4 -> true
            5 -> termsAccepted
            else -> false
        }
}

@HiltViewModel
class PetOnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val petRepository: PetRepository,
    private val liteRtModelPreloader: LiteRtModelPreloader,
    private val inferenceRuntime: InferenceRuntime,
    private val petModelResolver: PetModelResolver,
    private val modelAvailabilityGate: ModelAvailabilityGate,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetOnboardingUiState())
    val uiState: StateFlow<PetOnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val pet = petRepository.getPetState()
            val profile = userPreferencesRepository.petOnboardingProfile.first()
            val personality = userPreferencesRepository.petPersonality.first()
            _uiState.update {
                it.copy(
                    petName = pet.name.ifBlank { "nibbli" },
                    caretakerName = profile.caretakerName,
                    aboutYou = profile.aboutYou,
                    companionGoal = profile.companionGoal,
                    personality = personality,
                )
            }
        }
    }

    fun updateCaretakerName(value: String) {
        _uiState.update { it.copy(caretakerName = value) }
    }

    fun updatePetName(value: String) {
        _uiState.update { it.copy(petName = value) }
    }

    fun updatePersonality(personality: PetPersonality) {
        _uiState.update { it.copy(personality = personality) }
    }

    fun updateAboutYou(value: String) {
        _uiState.update { it.copy(aboutYou = value) }
    }

    fun updateCompanionGoal(value: String) {
        _uiState.update { it.copy(companionGoal = value) }
    }

    fun updateTermsAccepted(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.stepIndex >= state.stepCount - 1) state
            else state.copy(stepIndex = state.stepIndex + 1)
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.stepIndex <= 0) state
            else state.copy(stepIndex = state.stepIndex - 1)
        }
    }

    fun complete(onFinished: () -> Unit) {
        val state = _uiState.value
        if (state.isSaving || !state.termsAccepted) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val profile = PetOnboardingProfile(
                    caretakerName = state.caretakerName.trim(),
                    aboutYou = state.aboutYou.trim(),
                    companionGoal = state.companionGoal.trim(),
                    completed = true,
                )
                userPreferencesRepository.setPetOnboardingProfile(profile)
                userPreferencesRepository.setTermsAccepted(System.currentTimeMillis())
                userPreferencesRepository.setPetPersonality(state.personality)
                val pet = petRepository.getPetState()
                val trimmedPetName = state.petName.trim().ifBlank { "nibbli" }
                if (pet.name != trimmedPetName) {
                    petRepository.savePetState(pet.copy(name = trimmedPetName))
                }
                liteRtModelPreloader.invalidate()
                runCatching {
                    if (modelAvailabilityGate.hasUsableModel()) {
                        inferenceRuntime.resetHomeTalkSession(petModelResolver.resolve())
                    }
                }
                liteRtModelPreloader.preloadPrimaryModel(force = true)
                onFinished()
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}