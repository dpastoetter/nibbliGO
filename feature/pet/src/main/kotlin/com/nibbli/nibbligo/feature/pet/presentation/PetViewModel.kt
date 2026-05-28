package com.nibbli.nibbligo.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PetUiState(
    val petState: PetState = PetState(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PetViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val petEventBus: PetEventBus,
    private val engine: PetSimulationEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var state = petRepository.getPetState()
            state = engine.applyTimeDecay(state, System.currentTimeMillis())
            petRepository.savePetState(state)
            _uiState.update { it.copy(petState = state, isLoading = false) }

            petEventBus.events.collect { event ->
                val updated = engine.onPetEvent(_uiState.value.petState, event)
                persist(updated)
            }
        }
    }

    fun onInteraction(interaction: PetInteraction) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = engine.interact(_uiState.value.petState, interaction, now)
            persist(updated)
        }
    }

    private suspend fun persist(state: PetState) {
        petRepository.savePetState(state)
        _uiState.update { it.copy(petState = state) }
    }
}
