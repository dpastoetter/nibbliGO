package com.nibbli.nibbligo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ParentalControlsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes whether a destination should be PIN-gated, plus PIN verification. */
@HiltViewModel
class ParentalGateViewModel @Inject constructor(
    private val parentalControlsRepository: ParentalControlsRepository,
) : ViewModel() {

    /** True only when restriction is on AND a PIN exists to enforce it. */
    val gateActive: StateFlow<Boolean> =
        combine(
            parentalControlsRepository.restrictAdultFeatures,
            parentalControlsRepository.pinHash,
        ) { restrict, hash -> restrict && hash != null }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    suspend fun verify(pin: String): Boolean = parentalControlsRepository.verifyPin(pin)
}
