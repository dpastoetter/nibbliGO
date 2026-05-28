package com.nibbli.nibbligo.feature.benchmark.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nibbli.nibbligo.core.domain.repository.BenchmarkRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.model.BenchmarkRun
import com.nibbli.nibbligo.core.model.RuntimeResult
import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BenchmarkUiState(
    val installedModelIds: List<String> = emptyList(),
    val selectedModelId: String? = null,
    val history: List<BenchmarkRun> = emptyList(),
    val isRunning: Boolean = false,
    val lastResult: String? = null,
)

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val benchmarkRepository: BenchmarkRepository,
    private val inferenceRuntime: InferenceRuntime,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val installed = modelRepository.getInstalledModelIds()
            _uiState.update { it.copy(installedModelIds = installed, selectedModelId = installed.firstOrNull()) }
            benchmarkRepository.observeRuns().collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun selectModel(modelId: String) = _uiState.update { it.copy(selectedModelId = modelId) }

    fun runBenchmark() {
        val modelId = _uiState.value.selectedModelId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunning = true) }
            when (val result = inferenceRuntime.runBenchmark(modelId)) {
                is RuntimeResult.Success -> {
                    val run = BenchmarkRun(
                        modelId = modelId,
                        metrics = result.data,
                        timestampMillis = System.currentTimeMillis(),
                    )
                    benchmarkRepository.saveRun(run)
                    _uiState.update {
                        it.copy(
                            isRunning = false,
                            lastResult = "TTFT ${result.data.timeToFirstTokenMs}ms · " +
                                "${result.data.tokensPerSecond} tok/s · " +
                                result.data.thermalNote,
                        )
                    }
                }
                is RuntimeResult.Error -> _uiState.update { it.copy(isRunning = false, lastResult = result.message) }
                else -> _uiState.update { it.copy(isRunning = false, lastResult = "Failed") }
            }
        }
    }
}
