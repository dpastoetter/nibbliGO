package com.nibbli.nibbligo.core.domain.pet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Delivers deep-link payloads (e.g. Catch challenge scores) to Home. */
@Singleton
class PetDeepLinkBus @Inject constructor() {
    private val _pendingCatchChallengeScore = MutableStateFlow<Int?>(null)
    val pendingCatchChallengeScore: StateFlow<Int?> = _pendingCatchChallengeScore.asStateFlow()

    fun submitCatchChallenge(score: Int) {
        if (score > 0) _pendingCatchChallengeScore.value = score
    }

    fun consumeCatchChallenge(): Int? {
        val score = _pendingCatchChallengeScore.value ?: return null
        _pendingCatchChallengeScore.value = null
        return score
    }
}
