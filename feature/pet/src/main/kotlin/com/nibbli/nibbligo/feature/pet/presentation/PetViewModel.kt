package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.assist.AssistVoiceRequestBus
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.pet.llm.PetReactionPort
import com.nibbli.nibbligo.core.pet.llm.PetReactionRequest
import com.nibbli.nibbligo.core.pet.llm.PetStatusSnapshot
import com.nibbli.nibbligo.core.model.PetNeedRules
import com.nibbli.nibbligo.feature.pet.domain.PetDiaryExporter
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetSnapshot
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class PetUiState(
    val petState: PetState = PetState(),
    val isLoading: Boolean = true,
    val isGeneratingDialogue: Boolean = false,
    val showTalkSheet: Boolean = false,
    val showMinigame: Boolean = false,
    val agentToast: String? = null,
    val statusMessage: String? = null,
    val recentDialogue: List<String> = emptyList(),
    val isVoiceListening: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val petRepository: PetRepository,
    private val petEventBus: PetEventBus,
    private val petReactionPort: PetReactionPort,
    private val engine: PetSimulationEngine,
    private val assistVoiceRequestBus: AssistVoiceRequestBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()
    private val homeActive = MutableStateFlow(false)

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
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        viewModelScope.launch {
            petEventBus.events.collect { event ->
                val updated = engine.onPetEvent(_uiState.value.petState, event)
                persist(updated)
                val toast = when {
                    event.javaClass.simpleName.contains("Agent") -> "nibbli noticed your agent work!"
                    else -> null
                }
                _uiState.update { it.copy(agentToast = toast) }
            }
        }
        startMoodPulseLoop()
    }

    /** Called from PetHomeScreen when the tab is visible and lifecycle is resumed. */
    fun setHomeActive(active: Boolean) {
        homeActive.value = active
    }

    private fun startMoodPulseLoop() {
        viewModelScope.launch {
            combine(
                homeActive,
                _uiState.map { it.petState.isAwakeForMoodPulse }.distinctUntilChanged(),
            ) { home, awake -> home && awake }
                .flatMapLatest { active ->
                    if (!active) {
                        flow { }
                    } else {
                        flow {
                            delay(MOOD_PULSE_INTERVAL_MS)
                            while (true) {
                                emit(Unit)
                                delay(MOOD_PULSE_INTERVAL_MS)
                            }
                        }
                    }
                }
                .collect {
                    maybeGenerateMoodPulse()
                }
        }
    }

    private suspend fun maybeGenerateMoodPulse() {
        val ui = _uiState.value
        if (!homeActive.value || !isScreenInteractive()) return
        if (!ui.petState.isAwakeForMoodPulse) return
        if (ui.isLoading || ui.isGeneratingDialogue || ui.isVoiceListening ||
            ui.showTalkSheet || ui.showMinigame
        ) {
            return
        }

        val now = System.currentTimeMillis()
        val tick = engine.tick(ui.petState, now)
        val state = tick.state
        if (tick.state != ui.petState) {
            persist(state)
        }
        generateReaction(state, moodPulse = true)
    }

    private fun isScreenInteractive(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
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
            val tick = engine.tick(_uiState.value.petState, now)
            val freshState = tick.state
            if (freshState != _uiState.value.petState) {
                persist(freshState)
            }
            if (PetStatusSnapshot.isStatusQuestion(message)) {
                val reply = PetNeedRules.statusReply(freshState, now)
                val talkResult = engine.interact(freshState, PetInteraction.TALK, now)
                persist(talkResult.state.copy(dialogueLine = reply))
                return@launch
            }
            generateReaction(freshState, userMessage = message)
            val talkResult = engine.interact(freshState, PetInteraction.TALK, now)
            val reply = _uiState.value.petState.dialogueLine
            persist(talkResult.state.copy(dialogueLine = reply))
        }
    }

    fun onPetTapped() {
        val pet = _uiState.value.petState
        if (!pet.isAlive) return
        viewModelScope.launch {
            val lines = listOf("That tickles!", "Hehe!", "Nice pets!", "*happy wiggle*")
            val updated = pet.copy(
                stats = pet.stats.copy(
                    mood = (pet.stats.mood + 3).coerceAtMost(100),
                    trust = (pet.stats.trust + 2).coerceAtMost(100),
                ).clamped(),
                dialogueLine = lines.random(),
            )
            persist(updated)
        }
    }

    fun dismissTalkSheet() {
        _uiState.update { it.copy(showTalkSheet = false) }
    }

    fun onEquipCosmetic(cosmetic: PetCosmetic?) {
        viewModelScope.launch {
            val pet = _uiState.value.petState
            if (cosmetic != null && cosmetic !in pet.unlockedCosmetics) return@launch
            persist(pet.copy(equippedCosmetic = cosmetic))
        }
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

    fun setVoiceListening(listening: Boolean) {
        _uiState.update { it.copy(isVoiceListening = listening) }
    }

    fun submitVoiceToAssist(transcript: String) {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return
        assistVoiceRequestBus.submitVoiceMessage(trimmed)
        _uiState.update {
            it.copy(statusMessage = "Sent to Assist — on-device agent is thinking…")
        }
    }

    fun onVoiceAssistError(message: String) {
        _uiState.update {
            it.copy(
                isVoiceListening = false,
                statusMessage = message,
            )
        }
    }

    private suspend fun generateReaction(
        state: PetState,
        lastAction: String? = null,
        userMessage: String? = null,
        moodPulse: Boolean = false,
    ) {
        if (state.condition == PetCondition.DEAD) return
        _uiState.update { it.copy(isGeneratingDialogue = true) }
        val timeoutMs = if (moodPulse) MOOD_PULSE_TIMEOUT_MS else GENERATION_TIMEOUT_MS
        try {
            val reaction = withTimeoutOrNull(timeoutMs) {
                petReactionPort.generate(
                    PetReactionRequest(
                        state = state,
                        lastAction = lastAction,
                        userMessage = userMessage,
                        moodPulse = moodPulse,
                    ),
                )
            }
            if (reaction == null) {
                if (!moodPulse) {
                    persist(state.copy(dialogueLine = "I got distracted… try talking again?"))
                }
                return
            }
            val expression = reaction.suggestedExpression ?: state.expression
            val updated = state.copy(
                dialogueLine = reaction.dialogue,
                expression = expression,
            )
            persist(updated)
        } finally {
            _uiState.update { it.copy(isGeneratingDialogue = false) }
        }
    }

    private companion object {
        private const val GENERATION_TIMEOUT_MS = 90_000L
        private const val MOOD_PULSE_TIMEOUT_MS = 45_000L
        private const val MOOD_PULSE_INTERVAL_MS = 60_000L
    }

    private suspend fun persist(state: PetState) {
        petRepository.savePetState(state)
        PetWidgetSnapshot.write(context, state)
        PetWidgetUpdater.refresh(context)
        _uiState.update { current ->
            val dialogueChanged = state.dialogueLine != current.petState.dialogueLine &&
                state.dialogueLine.isNotBlank()
            val recent = if (dialogueChanged) {
                (listOf(current.petState.dialogueLine) + current.recentDialogue)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(2)
            } else {
                current.recentDialogue
            }
            current.copy(petState = state, recentDialogue = recent)
        }
    }
}
