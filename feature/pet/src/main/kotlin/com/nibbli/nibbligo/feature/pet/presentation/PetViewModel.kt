package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.pet.llm.PetReactionPort
import com.nibbli.nibbligo.core.pet.llm.PetReactionRequest
import com.nibbli.nibbligo.feature.pet.domain.PetDiaryExporter
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetSnapshot
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PetUiState(
    val petState: PetState = PetState(),
    val isLoading: Boolean = true,
    val isGeneratingDialogue: Boolean = false,
    val showTalkSheet: Boolean = false,
    val showMinigame: Boolean = false,
    val agentToast: String? = null,
    val statusMessage: String? = null,
)

@HiltViewModel
class PetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val petRepository: PetRepository,
    private val petEventBus: PetEventBus,
    private val petReactionPort: PetReactionPort,
    private val engine: PetSimulationEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                var state = petRepository.getPetState()
                val tick = engine.tick(state, System.currentTimeMillis())
                state = tick.state
                tick.templateDialogue?.let { state = state.copy(dialogueLine = it) }
                petRepository.savePetState(state)
                _uiState.update { it.copy(petState = state) }
                if (tick.welcomeBack) {
                    generateReaction(state, lastAction = "returning after a while")
                }

                petEventBus.events.collect { event ->
                    val updated = engine.onPetEvent(_uiState.value.petState, event)
                    persist(updated)
                    val toast = when {
                        event.javaClass.simpleName.contains("Agent") -> "nibbli noticed your agent work!"
                        else -> null
                    }
                    _uiState.update { it.copy(agentToast = toast) }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onInteraction(interaction: PetInteraction) {
        if (interaction == PetInteraction.TALK) {
            _uiState.update { it.copy(showTalkSheet = true) }
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val result = engine.interact(_uiState.value.petState, interaction, now)
            persist(result.state.copy(dialogueLine = result.templateDialogue))
            generateReaction(result.state, lastAction = interaction.name.lowercase().replace('_', ' '))
        }
    }

    fun onTalkSend(message: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val talkResult = engine.interact(_uiState.value.petState, PetInteraction.TALK, now)
            persist(talkResult.state)
            generateReaction(talkResult.state, userMessage = message)
        }
    }

    fun dismissTalkSheet() {
        _uiState.update { it.copy(showTalkSheet = false) }
    }

    fun openMinigame() {
        _uiState.update { it.copy(showMinigame = true) }
    }

    fun onMinigameWin() {
        viewModelScope.launch {
            val updated = engine.applyMinigameWin(_uiState.value.petState, System.currentTimeMillis())
            persist(updated)
            generateReaction(updated, lastAction = "winning the catch game")
        }
    }

    fun dismissMinigame() {
        _uiState.update { it.copy(showMinigame = false) }
    }

    fun hatchNewEgg() {
        viewModelScope.launch {
            val egg = engine.hatchNewEgg(_uiState.value.petState)
            persist(egg)
            _uiState.update { it.copy(statusMessage = "A new egg appeared!") }
        }
    }

    fun exportDiary(): Intent {
        val markdown = PetDiaryExporter.exportMarkdown(_uiState.value.petState)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "nibbli diary")
            putExtra(Intent.EXTRA_TEXT, markdown)
        }
    }

    fun clearAgentToast() {
        _uiState.update { it.copy(agentToast = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private suspend fun generateReaction(
        state: PetState,
        lastAction: String? = null,
        userMessage: String? = null,
    ) {
        if (state.condition == PetCondition.DEAD) return
        _uiState.update { it.copy(isGeneratingDialogue = true) }
        val reaction = petReactionPort.generate(
            PetReactionRequest(
                state = state,
                lastAction = lastAction,
                userMessage = userMessage,
            ),
        )
        val expression = reaction.suggestedExpression ?: state.expression
        val updated = state.copy(
            dialogueLine = reaction.dialogue,
            expression = expression,
        )
        persist(updated)
        _uiState.update { it.copy(isGeneratingDialogue = false) }
    }

    private suspend fun persist(state: PetState) {
        petRepository.savePetState(state)
        PetWidgetSnapshot.write(context, state)
        PetWidgetUpdater.refresh(context)
        _uiState.update { it.copy(petState = state) }
    }
}
