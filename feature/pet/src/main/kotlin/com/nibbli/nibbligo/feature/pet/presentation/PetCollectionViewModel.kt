package com.nibbli.nibbligo.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.feature.pet.ui.collection.LcdCollectionEntry
import com.nibbli.nibbligo.feature.pet.ui.collection.lcdCollectionEntries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PetCollectionUiState(
    val entries: List<LcdCollectionEntry> = emptyList(),
    val unlockedCount: Int = 0,
)

@HiltViewModel
class PetCollectionViewModel @Inject constructor(
    private val petRepository: PetRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetCollectionUiState())
    val uiState: StateFlow<PetCollectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            petRepository.observePetState().collect { pet ->
                val entries = pet.lcdCollectionEntries()
                _uiState.update {
                    PetCollectionUiState(
                        entries = entries,
                        unlockedCount = entries.count { entry -> entry.unlocked },
                    )
                }
            }
        }
    }
}
