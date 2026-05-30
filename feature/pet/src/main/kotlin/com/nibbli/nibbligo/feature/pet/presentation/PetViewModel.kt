package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.assist.AssistVoiceRequestBus
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.core.model.PetCosmetic
import com.nibbli.nibbligo.core.model.PetEvent
import com.nibbli.nibbligo.core.model.PetInteraction
import com.nibbli.nibbligo.core.model.PetMoodPulseMode
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.pet.llm.PetReaction
import com.nibbli.nibbligo.core.pet.llm.PetMemoryWriter
import com.nibbli.nibbligo.core.pet.llm.PetReactionParser
import com.nibbli.nibbligo.core.pet.llm.PetReactionPort
import com.nibbli.nibbligo.core.pet.llm.PetReactionRequest
import com.nibbli.nibbligo.core.pet.llm.PetReactionStreamEvent
import com.nibbli.nibbligo.feature.pet.domain.PetDiaryExporter
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetSnapshot
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val engine: PetSimulationEngine,
    private val assistVoiceRequestBus: AssistVoiceRequestBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetUiState())
    val uiState: StateFlow<PetUiState> = _uiState.asStateFlow()
    private val homeActive = MutableStateFlow(false)
    private var lastReactionAtMillis = 0L
    private val pendingActivityHints = mutableListOf<String>()
    private var activityReactionJob: Job? = null
    private var petInferenceJob: Job? = null
    private var warmLoadDeferred: Deferred<Unit>? = null
    private var userStoppedGeneration = false

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
                if (tick.evolved) {
                    generateReaction(state, lastAction = "evolving to ${state.stage.name.lowercase()}")
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
                scheduleActivityReaction(event)
            }
        }
        startMoodPulseLoop()
    }

    fun setHomeActive(active: Boolean) {
        homeActive.value = active
        if (active) {
            warmLoadDeferred = viewModelScope.async(Dispatchers.Default) {
                petReactionPort.warmLoad()
            }
        } else {
            warmLoadDeferred?.cancel()
            warmLoadDeferred = null
        }
    }

    private suspend fun awaitWarmLoad() {
        withTimeoutOrNull(WARM_LOAD_WAIT_MS) {
            warmLoadDeferred?.await()
        }
    }

    private fun startMoodPulseLoop() {
        viewModelScope.launch {
            combine(
                homeActive,
                _uiState.map { it.petState.isAwakeForMoodPulse }.distinctUntilChanged(),
                userPreferencesRepository.petMoodPulseMode,
            ) { home, awake, mode ->
                val active = home && awake
                active to mode
            }
                .flatMapLatest { (active, mode) ->
                    if (!active || mode == PetMoodPulseMode.OFF) {
                        flow { }
                    } else {
                        val interval = mode.intervalMs
                        flow {
                            delay(interval)
                            while (true) {
                                emit(Unit)
                                delay(interval)
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
        if (System.currentTimeMillis() - lastReactionAtMillis < MOOD_PULSE_COOLDOWN_MS) return
        if (petInferenceJob?.isActive == true) return

        val now = System.currentTimeMillis()
        val tick = engine.tick(ui.petState, now)
        val state = tick.state
        if (tick.state != ui.petState) {
            persist(state)
        }
        if (tick.evolved) {
            launchBackgroundReaction {
                generateReaction(state, lastAction = "evolving to ${state.stage.name.lowercase()}")
            }
            return
        }
        launchBackgroundReaction {
            generateReaction(state, moodPulse = true)
        }
    }

    private fun isScreenInteractive(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    private fun canGenerateOnHome(): Boolean {
        val ui = _uiState.value
        return homeActive.value &&
            ui.petState.isAlive &&
            ui.petState.isAwakeForMoodPulse &&
            !ui.isLoading &&
            !ui.isGeneratingDialogue &&
            !ui.isVoiceListening &&
            !ui.showTalkSheet &&
            !ui.showMinigame
    }

    private fun scheduleActivityReaction(event: PetEvent) {
        viewModelScope.launch {
            if (!userPreferencesRepository.petCommentOnAgentWork.first()) return@launch
            val hint = activityHintFor(event) ?: return@launch
            if (!homeActive.value) return@launch

            synchronized(pendingActivityHints) {
                pendingActivityHints.add(hint)
            }
            activityReactionJob?.cancel()
            activityReactionJob = viewModelScope.launch {
                delay(ACTIVITY_REACTION_DEBOUNCE_MS)
                val hints = synchronized(pendingActivityHints) {
                    val combined = pendingActivityHints.distinct().joinToString("; ")
                    pendingActivityHints.clear()
                    combined
                }
                if (hints.isBlank() || !canGenerateOnHome()) return@launch
                if (petInferenceJob?.isActive == true) return@launch
                generateReaction(
                    _uiState.value.petState,
                    activityHint = hints,
                )
            }
        }
    }

    private fun activityHintFor(event: PetEvent): String? = when (event) {
        PetEvent.AgentStepCompleted ->
            "Your human just finished an agent task on-device."
        is PetEvent.NewModelTried ->
            "They tried a new local model (${event.modelId})."
        PetEvent.PromptLabRun ->
            "They experimented in Prompt Lab."
        PetEvent.ActionCompleted ->
            "They completed an on-device action."
        PetEvent.AssistantSuccess ->
            "Their assistant gave a helpful answer."
        is PetEvent.SkillInvoked ->
            "They used skill ${event.skillId}."
        PetEvent.NeglectTick,
        PetEvent.BecameSick,
        is PetEvent.Evolution,
        -> null
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
            launchBackgroundReaction {
                generateReaction(result.state, lastAction = interaction.name.lowercase().replace('_', ' '))
            }
        }
    }

    fun onQuickChip(phrase: String) {
        if (phrase.equals(LETS_PLAY_CHIP, ignoreCase = true)) {
            openMinigame()
            return
        }
        onTalkSend(phrase)
    }

    fun onTalkSend(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.equals(LETS_PLAY_CHIP, ignoreCase = true)) {
            openMinigame()
            return
        }
        activityReactionJob?.cancel()
        userStoppedGeneration = false
        petInferenceJob?.cancel()
        petInferenceJob = viewModelScope.launch(Dispatchers.Default) {
            awaitWarmLoad()
            val now = System.currentTimeMillis()
            val tick = engine.tick(_uiState.value.petState, now)
            val talkResult = engine.interact(tick.state, PetInteraction.TALK, now)
            generateReaction(
                state = talkResult.state,
                userMessage = trimmed,
                userInitiated = true,
            )
        }
    }

    fun stopGeneration() {
        if (!_uiState.value.isGeneratingDialogue) return
        userStoppedGeneration = true
        petInferenceJob?.cancel()
        petInferenceJob = null
        _uiState.update { it.copy(isGeneratingDialogue = false) }
    }

    fun onPetTapped() {
        val pet = _uiState.value.petState
        if (!pet.isAlive) return
        viewModelScope.launch {
            val withStats = pet.copy(
                stats = pet.stats.copy(
                    mood = (pet.stats.mood + 3).coerceAtMost(100),
                    trust = (pet.stats.trust + 2).coerceAtMost(100),
                ).clamped(),
            )
            persist(withStats)
            launchBackgroundReaction {
                generateReaction(withStats, lastAction = "gentle pets", quickTimeout = true)
            }
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
            launchBackgroundReaction {
                generateReaction(updated, lastAction = "winning the catch game")
            }
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

    private fun launchBackgroundReaction(block: suspend () -> Unit) {
        if (petInferenceJob?.isActive == true) return
        lateinit var job: Job
        job = viewModelScope.launch(Dispatchers.Default) {
            try {
                block()
            } finally {
                if (petInferenceJob == job) {
                    petInferenceJob = null
                }
            }
        }
        petInferenceJob = job
    }

    private suspend fun generateReaction(
        state: PetState,
        lastAction: String? = null,
        userMessage: String? = null,
        moodPulse: Boolean = false,
        activityHint: String? = null,
        quickTimeout: Boolean = false,
        userInitiated: Boolean = false,
    ) {
        if (state.condition == PetCondition.DEAD) return
        if (!userInitiated && petInferenceJob?.isActive == true) return
        if (_uiState.value.isGeneratingDialogue && !userInitiated) return

        val timeoutMs = when {
            quickTimeout -> TAP_REACTION_TIMEOUT_MS
            moodPulse -> MOOD_PULSE_TIMEOUT_MS
            userInitiated -> USER_TALK_TIMEOUT_MS
            else -> GENERATION_TIMEOUT_MS
        }
        val ui = _uiState.value
        val recentLines = buildList {
            addAll(ui.recentDialogue)
            if (ui.petState.dialogueLine.isNotBlank()) add(ui.petState.dialogueLine)
        }.filter { it.isNotBlank() }.distinct().takeLast(2)

        val request = PetReactionRequest(
            state = state,
            lastAction = lastAction,
            userMessage = userMessage,
            moodPulse = moodPulse,
            recentLines = recentLines,
            activityHint = activityHint,
        )

        if (userInitiated && userMessage != null) {
            val userLine = "You: $userMessage"
            _uiState.update {
                it.copy(
                    isGeneratingDialogue = true,
                    petState = state.copy(dialogueLine = ""),
                    recentDialogue = (listOf(userLine) + it.recentDialogue)
                        .filter { line -> line.isNotBlank() }
                        .distinct()
                        .take(2),
                )
            }
            generateReactionStreaming(state, request, timeoutMs, moodPulse)
            return
        }

        _uiState.update { it.copy(isGeneratingDialogue = true) }
        try {
            val reaction = withTimeoutOrNull(timeoutMs) {
                withContext(Dispatchers.Default) {
                    petReactionPort.generate(request)
                }
            }
            if (reaction == null) {
                if (userStoppedGeneration || moodPulse) return
                Log.w(TAG, "Pet reaction timed out (non-streaming)")
                applyReaction(state, request, PetReactionParser.fallback(request))
                return
            }
            applyReaction(state, request, reaction)
        } finally {
            _uiState.update { it.copy(isGeneratingDialogue = false) }
        }
    }

    private suspend fun generateReactionStreaming(
        state: PetState,
        request: PetReactionRequest,
        timeoutMs: Long,
        moodPulse: Boolean,
    ) {
        try {
            awaitWarmLoad()
            var finalReaction: PetReaction? = null
            withTimeoutOrNull(timeoutMs) {
                withContext(Dispatchers.Default) {
                    petReactionPort.generateStream(request).collect { event ->
                        when (event) {
                            is PetReactionStreamEvent.Token -> {
                                _uiState.update {
                                    it.copy(petState = it.petState.copy(dialogueLine = event.text))
                                }
                            }
                            is PetReactionStreamEvent.Done -> finalReaction = event.reaction
                        }
                    }
                }
            }
            val reaction = finalReaction
            if (reaction == null) {
                if (userStoppedGeneration || moodPulse) return
                Log.w(TAG, "Pet talk stream ended without a reaction (timeout or cancel)")
                applyReaction(state, request, PetReactionParser.fallback(request))
                return
            }
            applyReaction(state, request, reaction)
        } finally {
            _uiState.update { it.copy(isGeneratingDialogue = false) }
        }
    }

    private suspend fun applyReaction(
        state: PetState,
        request: PetReactionRequest,
        reaction: PetReaction,
    ) {
        lastReactionAtMillis = System.currentTimeMillis()
        val expression = reaction.suggestedExpression ?: state.expression
        var updated = state.copy(
            dialogueLine = reaction.dialogue,
            expression = expression,
        )
        updated = PetMemoryWriter.withNewFact(updated, request, reaction.dialogue)
        persist(updated)
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

    private companion object {
        private const val TAG = "PetViewModel"
        private const val GENERATION_TIMEOUT_MS = 90_000L
        private const val USER_TALK_TIMEOUT_MS = 30_000L
        private const val TAP_REACTION_TIMEOUT_MS = 30_000L
        private const val MOOD_PULSE_TIMEOUT_MS = 45_000L
        private const val MOOD_PULSE_COOLDOWN_MS = 45_000L
        private const val ACTIVITY_REACTION_DEBOUNCE_MS = 8_000L
        private const val WARM_LOAD_WAIT_MS = 15_000L
        private const val LETS_PLAY_CHIP = "Let's play!"
    }
}
