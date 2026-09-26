package com.nibbli.nibbligo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ParentalControlsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes whether a destination should be PIN-gated, plus PIN verification. */
@HiltViewModel
class ParentalGateViewModel @Inject constructor(
    private val parentalControlsRepository: ParentalControlsRepository,
) : ViewModel() {

    enum class VerifyResult { Success, Incorrect, Locked }

    /** True only when restriction is on AND a PIN exists to enforce it. */
    val gateActive: StateFlow<Boolean> =
        combine(
            parentalControlsRepository.restrictAdultFeatures,
            parentalControlsRepository.pinHash,
        ) { restrict, hash -> restrict && hash != null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val pinLockedUntilEpochMs: StateFlow<Long> =
        parentalControlsRepository.pinLockedUntilEpochMs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    suspend fun verify(pin: String): VerifyResult {
        val lockedUntil = parentalControlsRepository.pinLockedUntilEpochMs.first()
        if (lockedUntil > System.currentTimeMillis()) return VerifyResult.Locked
        return if (parentalControlsRepository.verifyPin(pin)) {
            VerifyResult.Success
        } else {
            val stillLocked = parentalControlsRepository.pinLockedUntilEpochMs.first()
            if (stillLocked > System.currentTimeMillis()) VerifyResult.Locked else VerifyResult.Incorrect
        }
    }
}
