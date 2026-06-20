package com.nibbli.nibbligo.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.ParentalControlsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentControlsUiState(
    val pinSet: Boolean = false,
    val restrictAdultFeatures: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ParentControlsViewModel @Inject constructor(
    private val parentalControlsRepository: ParentalControlsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentControlsUiState())
    val uiState: StateFlow<ParentControlsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                parentalControlsRepository.pinHash,
                parentalControlsRepository.restrictAdultFeatures,
            ) { hash, restrict -> (hash != null) to restrict }
                .collect { (pinSet, restrict) ->
                    _uiState.update { it.copy(pinSet = pinSet, restrictAdultFeatures = restrict) }
                }
        }
    }

    fun setPin(rawPin: String) {
        val trimmed = rawPin.trim()
        if (trimmed.length < 4 || !trimmed.all { it.isDigit() }) {
            _uiState.update { it.copy(message = "Enter a 4+ digit PIN.") }
            return
        }
        viewModelScope.launch {
            parentalControlsRepository.setPin(trimmed)
            _uiState.update { it.copy(message = "Parent PIN saved.") }
        }
    }

    fun removePin() {
        viewModelScope.launch {
            parentalControlsRepository.setPin(null)
            _uiState.update { it.copy(message = "Parent PIN removed.") }
        }
    }

    fun setRestrictAdultFeatures(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !parentalControlsRepository.isPinSet()) {
                _uiState.update { it.copy(message = "Set a parent PIN first.") }
                return@launch
            }
            parentalControlsRepository.setRestrictAdultFeatures(enabled)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
