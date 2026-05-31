package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.assist.AssistVoiceRequestBus
import com.nibbli.nibbligo.core.domain.event.PetEventBus
import com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
import com.nibbli.nibbligo.core.domain.pet.PetDeepLinkBus
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.model.LifeStage
import com.nibbli.nibbligo.core.model.PetCondition
import com.nibbli.nibbligo.feature.pet.ui.pixel.LcdPickerEntry
import com.nibbli.nibbligo.feature.pet.ui.pixel.applyPickerEquip
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
import com.nibbli.nibbligo.core.pet.llm.PetTalkChatRecorder
import com.nibbli.nibbligo.core.pet.llm.PetTalkLimits
import com.nibbli.nibbligo.core.pet.llm.LiteRtModelPreloader
import com.nibbli.nibbligo.feature.pet.domain.PetDiaryExporter
import com.nibbli.nibbligo.feature.pet.domain.PetEngagementEngine
import com.nibbli.nibbligo.feature.pet.domain.PetPostcard
import com.nibbli.nibbligo.feature.pet.domain.PetVisitQrCodec
import com.nibbli.nibbligo.feature.pet.domain.PetVisitQrExporter
import com.nibbli.nibbligo.feature.pet.domain.PetPostcardStore
import com.nibbli.nibbligo.feature.pet.domain.PetShareExporter
import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetSnapshot
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    val talkHistory: List<TalkHistoryEntry> = emptyList(),
    val talkLcdMode: Boolean = false,
    val isVoiceListening: Boolean = false,
    val petModelLabel: String = "No model",
    val petModelInstalled: Boolean = false,
    val isWarmingModel: Boolean = false,
    val evolutionShareStage: LifeStage? = null,
    val showPostcardSheet: Boolean = false,
    val visitPostcard: PetPostcard? = null,
    val catchGhostScore: Int? = null,
    val lastCatchScore: Int? = null,
    val lastCatchCombo: Int? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val petRepository: PetRepository,
    private val petEventBus: PetEventBus,
    private val petReactionPort: PetReactionPort,
    private val petTalkChatRecorder: PetTalkChatRecorder,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val engine: PetSimulationEngine,
    private val assistVoiceRequestBus: AssistVoiceRequestBus,
    private val liteRtModelPreloader: LiteRtModelPreloader,
    private val petDeepLinkBus: PetDeepLinkBus,
    private val modelAvailabilityGate: ModelAvailabilityGate,
) : ViewModel() {

    private val postcardStore = PetPostcardStore(context)

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
                val now = System.currentTimeMillis()
                state = PetEngagementEngine.onSessionOpen(state, now)
                val tick = engine.tick(state, now)
                state = tick.state
                tick.templateDialogue?.let { line -> state = state.copy(dialogueLine = line) }
                petRepository.savePetState(state)
                val visit = postcardStore.load()
                _uiState.update { it.copy(petState = state, visitPostcard = visit) }
                if (tick.evolved) {
                    _uiState.update { it.copy(evolutionShareStage = state.stage) }
                    launchBackgroundReaction {
                        generateReaction(state, lastAction = "evolving to ${state.stage.name.lowercase()}")
                    }
                } else if (tick.welcomeBack) {
                    launchBackgroundReaction {
                        generateReaction(state, lastAction = "returning after a while")
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                refreshPetModelLabel()
            }
        }
        viewModelScope.launch {
            petEventBus.events.collect { event ->
                val updated = engine.onPetEvent(_uiState.value.petState, event)
                persist(updated)
                val toast = when (event) {
                    PetEvent.AssistantSuccess -> "nibbli noticed your chat!"
                    else -> null
                }
                _uiState.update { it.copy(agentToast = toast) }
                scheduleActivityReaction(event)
            }
        }
        startMoodPulseLoop()
        viewModelScope.launch {
            userPreferencesRepository.petModelId.collect {
                refreshPetModelLabel()
            }
        }
        viewModelScope.launch {
            liteRtModelPreloader.isWarmingUp.collect { warming ->
                _uiState.update { it.copy(isWarmingModel = warming) }
            }
        }
        viewModelScope.launch {
            petDeepLinkBus.pendingCatchChallengeScore.collect { score ->
                if (score != null && score > 0) {
                    petDeepLinkBus.consumeCatchChallenge()
                    _uiState.update {
                        it.copy(catchGhostScore = score, showMinigame = true)
                    }
                }
            }
        }
    }

    private suspend fun refreshPetModelLabel() {
        val label = petReactionPort.activeModelDisplayName()
        val installed = modelAvailabilityGate.hasUsableModel()
        _uiState.update { it.copy(petModelLabel = label, petModelInstalled = installed) }
    }

    fun setHomeActive(active: Boolean) {
        homeActive.value = active
        if (active) {
            warmLoadDeferred = viewModelScope.async(Dispatchers.Default) {
                petReactionPort.warmLoad()
            }
            viewModelScope.launch { refreshPetModelLabel() }
        } else {
            warmLoadDeferred?.cancel()
            warmLoadDeferred = null
        }
    }

    private suspend fun awaitWarmLoad(): Boolean =
        withTimeoutOrNull(WARM_LOAD_WAIT_MS) {
            warmLoadDeferred?.await()
        } != null

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
            ui.isWarmingModel ||
            ui.showTalkSheet || ui.showMinigame || ui.talkLcdMode
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
            !ui.isWarmingModel &&
            !ui.showTalkSheet &&
            !ui.showMinigame &&
            !ui.talkLcdMode
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
            "Your human just had a good Chat conversation on-device."
        is PetEvent.NewModelTried ->
            "They tried a new local model (${event.modelId})."
        PetEvent.PromptLabRun ->
            "They tried something new on-device."
        PetEvent.ActionCompleted ->
            "They completed an on-device action."
        PetEvent.AssistantSuccess ->
            "They had a helpful Chat conversation on-device."
        is PetEvent.SkillInvoked ->
            "They used skill ${event.skillId}."
        PetEvent.NeglectTick,
        PetEvent.BecameSick,
        is PetEvent.Evolution,
        -> null
    }

    fun onInteraction(interaction: PetInteraction) {
        if (interaction == PetInteraction.TALK) {
            viewModelScope.launch {
                wakeIfSleeping()
                dismissTalkLcdMode()
                _uiState.update { it.copy(showTalkSheet = true) }
            }
            return
        }
        if (petInferenceJob?.isActive == true && _uiState.value.talkLcdMode) {
            return
        }
        dismissTalkLcdMode()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val result = engine.interact(_uiState.value.petState, interaction, now)
            var state = PetEngagementEngine.recordCareInteraction(result.state, interaction, now)
            persist(state.copy(dialogueLine = result.templateDialogue))
            launchBackgroundReaction {
                generateReaction(state, lastAction = interaction.name.lowercase().replace('_', ' '))
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
        if (_uiState.value.isWarmingModel) return
        if (trimmed.equals(LETS_PLAY_CHIP, ignoreCase = true)) {
            openMinigame()
            return
        }
        activityReactionJob?.cancel()
        userStoppedGeneration = false
        petInferenceJob?.cancel()
        petInferenceJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val now = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        talkLcdMode = true,
                        talkHistory = appendTalkHistory(
                            it.talkHistory,
                            TalkHistoryEntry(TalkHistoryRole.USER, trimmed),
                        ),
                    )
                }
                val warmLoad = async { awaitWarmLoad() }
                val talkState = async {
                    val tick = engine.tick(_uiState.value.petState, now)
                    val interacted = engine.interact(tick.state, PetInteraction.TALK, now)
                    PetEngagementEngine.recordTalk(interacted.state, now)
                }
                val warmLoaded = warmLoad.await()
                launch(Dispatchers.Default) {
                    val modelId = petTalkChatRecorder.resolvePetModelId()
                    petTalkChatRecorder.recordUserMessage(trimmed, modelId, now)
                }
                generateReaction(
                    state = talkState.await(),
                    userMessage = trimmed,
                    userInitiated = true,
                    coldStart = !warmLoaded,
                )
            } finally {
                if (petInferenceJob == coroutineContext[Job]) {
                    petInferenceJob = null
                }
                clearGeneratingDialogueIfIdle()
            }
        }
    }

    fun stopGeneration() {
        if (!_uiState.value.isGeneratingDialogue && petInferenceJob?.isActive != true) return
        userStoppedGeneration = true
        petInferenceJob?.cancel()
        petInferenceJob = null
        clearGeneratingDialogue()
    }

    fun onLcdActivity() {
        viewModelScope.launch {
            wakeIfSleeping()
        }
    }

    fun onPetTapped() {
        val pet = _uiState.value.petState
        if (!pet.isAlive) return
        viewModelScope.launch {
            val current = wakeIfSleeping() ?: pet
            val withStats = current.copy(
                stats = current.stats.copy(
                    mood = (current.stats.mood + 3).coerceAtMost(100),
                    trust = (current.stats.trust + 2).coerceAtMost(100),
                ).clamped(),
            )
            persist(withStats)
            launchBackgroundReaction {
                generateReaction(withStats, lastAction = "gentle pets", quickTimeout = true)
            }
        }
    }

    private suspend fun wakeIfSleeping(): PetState? {
        val pet = _uiState.value.petState
        if (pet.condition != PetCondition.SLEEPING || !pet.isAlive) return null
        val result = engine.interact(pet, PetInteraction.WAKE, System.currentTimeMillis())
        persist(result.state.copy(dialogueLine = result.templateDialogue))
        return result.state
    }

    fun dismissTalkSheet() {
        _uiState.update { it.copy(showTalkSheet = false) }
    }

    fun onEquipLcdItem(entry: LcdPickerEntry) {
        viewModelScope.launch {
            val pet = _uiState.value.petState
            val updated = pet.applyPickerEquip(entry) ?: return@launch
            persist(updated)
        }
    }

    fun openMinigame() {
        _uiState.update { it.copy(showMinigame = true) }
    }

    fun onMinigameWin() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = engine.applyMinigameWin(_uiState.value.petState, now)
            persist(updated)
            launchBackgroundReaction {
                generateReaction(updated, lastAction = "winning a mini-game")
            }
        }
    }

    fun onMinigameEnd(score: Int, bestCombo: Int, won: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = PetEngagementEngine.recordCatchGame(
                _uiState.value.petState,
                score,
                bestCombo,
                won,
                now,
            )
            persist(updated)
            _uiState.update {
                it.copy(lastCatchScore = score, lastCatchCombo = bestCombo)
            }
        }
    }

    fun shareTodayCard(shareContext: Context, onReady: (Intent) -> Unit) {
        val pet = _uiState.value.petState
        viewModelScope.launch {
            val intent = PetShareExporter.shareTodayCard(shareContext, pet)
            onReady(intent)
        }
    }

    fun shareCatchCard(shareContext: Context, onReady: (Intent) -> Unit) {
        val ui = _uiState.value
        viewModelScope.launch {
            val intent = PetShareExporter.shareCatchCard(
                shareContext,
                ui.petState,
                ui.lastCatchScore ?: ui.petState.engagement.catchHighScore,
                ui.lastCatchCombo ?: ui.petState.engagement.catchBestCombo,
            )
            onReady(intent)
        }
    }

    fun shareEvolutionCard(shareContext: Context, onReady: (Intent?) -> Unit) {
        val stage = _uiState.value.evolutionShareStage
        if (stage == null) {
            onReady(null)
            return
        }
        val pet = _uiState.value.petState
        viewModelScope.launch {
            val intent = PetShareExporter.shareEvolutionCard(shareContext, pet, stage)
            onReady(intent)
        }
    }

    fun shareQuoteCard(shareContext: Context, onReady: (Intent) -> Unit) {
        val pet = _uiState.value.petState
        viewModelScope.launch {
            val intent = PetShareExporter.shareQuoteCard(shareContext, pet, pet.dialogueLine)
            onReady(intent)
        }
    }

    fun dismissEvolutionSharePrompt() {
        _uiState.update { it.copy(evolutionShareStage = null) }
    }

    fun openPostcardSheet() {
        _uiState.update { it.copy(showPostcardSheet = true) }
    }

    fun dismissPostcardSheet() {
        _uiState.update { it.copy(showPostcardSheet = false) }
    }

    fun importVisitFromQr(raw: String) {
        val postcard = PetVisitQrCodec.decode(raw) ?: run {
            _uiState.update { it.copy(statusMessage = "Invalid visit QR code") }
            return
        }
        postcardStore.save(postcard)
        _uiState.update {
            it.copy(
                visitPostcard = postcard,
                showPostcardSheet = false,
                statusMessage = "Visiting ${postcard.senderName}'s nibbli!",
            )
        }
    }

    fun shareVisitQr(): Intent =
        PetVisitQrExporter.shareVisitQr(context, _uiState.value.petState)

    fun endPostcardVisit() {
        postcardStore.clear()
        _uiState.update { it.copy(visitPostcard = null) }
    }

    fun clearCatchGhost() {
        _uiState.update { it.copy(catchGhostScore = null) }
    }

    fun dismissMinigame() {
        _uiState.update { it.copy(showMinigame = false) }
    }

    fun hatchNewEgg() {
        viewModelScope.launch {
            val egg = engine.hatchNewEgg(_uiState.value.petState)
            persist(egg)
            _uiState.update {
                it.copy(
                    statusMessage = "A new egg appeared!",
                    talkLcdMode = false,
                    talkHistory = emptyList(),
                )
            }
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
            it.copy(statusMessage = "Sent to Chat — on-device model is thinking…")
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
        if (_uiState.value.talkLcdMode) return
        petInferenceJob?.cancel()
        lateinit var job: Job
        job = viewModelScope.launch {
            try {
                block()
            } finally {
                if (petInferenceJob == job) {
                    petInferenceJob = null
                }
                clearGeneratingDialogueIfIdle()
            }
        }
        petInferenceJob = job
    }

    private fun clearGeneratingDialogue() {
        _uiState.update { it.copy(isGeneratingDialogue = false) }
    }

    private fun clearGeneratingDialogueIfIdle() {
        if (petInferenceJob?.isActive == true) return
        clearGeneratingDialogue()
    }

    private fun markGeneratingDialogue() {
        _uiState.update { it.copy(isGeneratingDialogue = true) }
    }

    /** Inference finished; hide Stop before persistence work. */
    private fun finishGeneratingDialogue() {
        clearGeneratingDialogue()
    }

    private suspend fun generateReaction(
        state: PetState,
        lastAction: String? = null,
        userMessage: String? = null,
        moodPulse: Boolean = false,
        activityHint: String? = null,
        quickTimeout: Boolean = false,
        userInitiated: Boolean = false,
        coldStart: Boolean = false,
    ) {
        if (state.condition == PetCondition.DEAD) return
        if (_uiState.value.isGeneratingDialogue && !userInitiated) return
        if (_uiState.value.isWarmingModel && !userInitiated) return

        val timeoutMs = when {
            quickTimeout -> TAP_REACTION_TIMEOUT_MS
            moodPulse -> MOOD_PULSE_TIMEOUT_MS
            userInitiated && coldStart -> USER_TALK_COLD_TIMEOUT_MS
            userInitiated -> USER_TALK_TIMEOUT_MS
            else -> GENERATION_TIMEOUT_MS
        }
        val ui = _uiState.value
        val recentLines = talkHistoryToRecentLines(ui.talkHistory)

        val request = PetReactionRequest(
            state = state,
            lastAction = lastAction,
            userMessage = userMessage,
            moodPulse = moodPulse,
            recentLines = recentLines,
            activityHint = activityHint,
        )

        if (userInitiated && userMessage != null) {
            _uiState.update {
                it.copy(
                    talkLcdMode = true,
                    petState = state.copy(dialogueLine = ""),
                )
            }
            markGeneratingDialogue()
            generateReactionStreaming(state, request, timeoutMs, moodPulse)
            return
        }

        markGeneratingDialogue()
        try {
            val reaction = withTimeoutOrNull(timeoutMs) {
                withContext(Dispatchers.Default) {
                    petReactionPort.generate(request)
                }
            }
            finishGeneratingDialogue()
            if (reaction == null) {
                if (userStoppedGeneration || moodPulse) return
                Log.w(TAG, "Pet reaction timed out (non-streaming)")
                applyReaction(state, request, PetReactionParser.fallback(request))
                return
            }
            applyReaction(state, request, reaction)
        } finally {
            clearGeneratingDialogueIfIdle()
        }
    }

    private suspend fun generateReactionStreaming(
        state: PetState,
        request: PetReactionRequest,
        timeoutMs: Long,
        moodPulse: Boolean,
    ) {
        val streamStart = System.nanoTime()
        try {
            var finalReaction: PetReaction? = null
            var lastUiUpdateMs = 0L
            var pendingDialogue: String? = null
            fun flushDialogue(text: String) {
                _uiState.update {
                    it.copy(
                        petState = it.petState.copy(dialogueLine = text),
                        talkHistory = updateLastPetTalkEntry(it.talkHistory, text),
                    )
                }
            }
            withTimeoutOrNull(timeoutMs) {
                withContext(Dispatchers.Default) {
                    try {
                        petReactionPort.generateStream(request).collect { event ->
                            when (event) {
                                is PetReactionStreamEvent.Token -> {
                                    pendingDialogue = event.text
                                    val now = System.currentTimeMillis()
                                    if (now - lastUiUpdateMs >= STREAM_UI_MIN_INTERVAL_MS) {
                                        lastUiUpdateMs = now
                                        flushDialogue(event.text)
                                        pendingDialogue = null
                                    }
                                }
                                is PetReactionStreamEvent.Done -> finalReaction = event.reaction
                            }
                        }
                    } catch (e: CancellationException) {
                        if (userStoppedGeneration) return@withContext
                        throw e
                    }
                }
            }
            pendingDialogue?.let { flushDialogue(it) }
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                val elapsedMs = (System.nanoTime() - streamStart) / 1_000_000
                Log.d(TAG, "talkStream total=${elapsedMs}ms")
            }
            finishGeneratingDialogue()
            val reaction = finalReaction
            if (reaction == null) {
                if (userStoppedGeneration || moodPulse) return
                val streamed = _uiState.value.petState.dialogueLine.trim()
                if (request.userMessage != null && streamed.isNotBlank()) {
                    Log.w(TAG, "Pet talk stream ended without Done; keeping streamed dialogue")
                    applyReaction(
                        state,
                        request,
                        PetReaction(dialogue = streamed.take(PetTalkLimits.RUNAWAY_MAX_CHARS)),
                    )
                    return
                }
                Log.w(TAG, "Pet talk stream ended without a reaction (timeout or cancel)")
                applyReaction(state, request, PetReactionParser.fallback(request))
                return
            }
            applyReaction(state, request, reaction)
        } finally {
            clearGeneratingDialogueIfIdle()
        }
    }

    private suspend fun applyReaction(
        state: PetState,
        request: PetReactionRequest,
        reaction: PetReaction,
    ) {
        lastReactionAtMillis = System.currentTimeMillis()
        val resolved = if (request.userMessage != null) {
            PetReactionParser.reconcileTalkStream(
                reaction,
                _uiState.value.petState.dialogueLine,
            )
        } else {
            reaction
        }
        val expression = resolved.suggestedExpression ?: state.expression
        var updated = state.copy(
            dialogueLine = resolved.dialogue,
            expression = expression,
        )
        updated = PetMemoryWriter.withNewFact(updated, request, resolved.dialogue)
        if (request.userMessage != null) {
            _uiState.update {
                it.copy(
                    petState = updated,
                    talkHistory = updateLastPetTalkEntry(it.talkHistory, resolved.dialogue),
                )
            }
        } else {
            _uiState.update { it.copy(petState = updated) }
        }
        petRepository.savePetState(updated)
        PetWidgetSnapshot.write(context, updated)
        PetWidgetUpdater.refresh(context)
        if (request.userMessage != null) {
            val modelId = petTalkChatRecorder.resolvePetModelId()
            petTalkChatRecorder.recordAssistantMessage(resolved.dialogue, modelId)
        }
    }

    fun dismissTalkLcdMode() {
        _uiState.update { it.copy(talkLcdMode = false) }
    }

    private suspend fun persist(state: PetState) {
        val ui = _uiState.value
        val toPersist = if (ui.isGeneratingDialogue && ui.petState.dialogueLine.isNotBlank()) {
            state.copy(dialogueLine = ui.petState.dialogueLine)
        } else {
            state
        }
        petRepository.savePetState(toPersist)
        PetWidgetSnapshot.write(context, toPersist)
        PetWidgetUpdater.refresh(context)
        _uiState.update { it.copy(petState = toPersist) }
    }

    private companion object {
        private const val TAG = "PetViewModel"
        private const val GENERATION_TIMEOUT_MS = 90_000L
        private const val USER_TALK_TIMEOUT_MS = 90_000L
        private const val USER_TALK_COLD_TIMEOUT_MS = 120_000L
        private const val TAP_REACTION_TIMEOUT_MS = 30_000L
        private const val MOOD_PULSE_TIMEOUT_MS = 45_000L
        private const val MOOD_PULSE_COOLDOWN_MS = 45_000L
        private const val ACTIVITY_REACTION_DEBOUNCE_MS = 8_000L
        private const val WARM_LOAD_WAIT_MS = 15_000L
        private const val STREAM_UI_MIN_INTERVAL_MS = 33L
        private const val LETS_PLAY_CHIP = "Let's play!"
    }
}
