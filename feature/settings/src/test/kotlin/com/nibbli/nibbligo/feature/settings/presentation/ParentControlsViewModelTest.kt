package com.nibbli.nibbligo.feature.settings.presentation

import com.nibbli.nibbligo.core.domain.repository.ParentalControlsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParentControlsViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setPin_rejectsShortPin() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repo = FakeParentalControlsRepository()
        val viewModel = ParentControlsViewModel(repo)
        advanceUntilIdle()

        viewModel.setPin("12")
        advanceUntilIdle()

        assertEquals("Enter a 4+ digit PIN.", viewModel.uiState.value.message)
        assertFalse(repo.isPinSet())
    }

    @Test
    fun setPin_rejectsNonDigits() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repo = FakeParentalControlsRepository()
        val viewModel = ParentControlsViewModel(repo)

        viewModel.setPin("12ab")
        advanceUntilIdle()

        assertEquals("Enter a 4+ digit PIN.", viewModel.uiState.value.message)
    }

    @Test
    fun setPin_savesValidPin() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repo = FakeParentalControlsRepository()
        val viewModel = ParentControlsViewModel(repo)
        advanceUntilIdle()

        viewModel.setPin("1234")
        advanceUntilIdle()

        assertTrue(repo.isPinSet())
        assertTrue(viewModel.uiState.value.pinSet)
        assertEquals("Parent PIN saved.", viewModel.uiState.value.message)
    }

    @Test
    fun setRestrictAdultFeatures_requiresPin() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repo = FakeParentalControlsRepository()
        val viewModel = ParentControlsViewModel(repo)
        advanceUntilIdle()

        viewModel.setRestrictAdultFeatures(true)
        advanceUntilIdle()

        assertEquals("Set a parent PIN first.", viewModel.uiState.value.message)
        assertFalse(repo.restrictAdultFeaturesValue())
    }

    @Test
    fun removePin_clearsRestrictFlag() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repo = FakeParentalControlsRepository()
        repo.setPin("1234")
        repo.setRestrictAdultFeatures(true)
        val viewModel = ParentControlsViewModel(repo)
        advanceUntilIdle()

        viewModel.removePin()
        advanceUntilIdle()

        assertFalse(repo.isPinSet())
        assertFalse(viewModel.uiState.value.pinSet)
        assertFalse(viewModel.uiState.value.restrictAdultFeatures)
    }

    @Test
    fun gateActive_onlyWhenRestrictAndPinSet() = runTest {
        val repo = FakeParentalControlsRepository()
        assertFalse(gateActive(repo))

        repo.setRestrictAdultFeatures(true)
        assertFalse(gateActive(repo))

        repo.setPin("1234")
        assertTrue(gateActive(repo))

        repo.setPin(null)
        assertFalse(gateActive(repo))
    }

    private suspend fun gateActive(repo: FakeParentalControlsRepository): Boolean =
        combine(repo.restrictAdultFeatures, repo.pinHash) { restrict, hash ->
            restrict && hash != null
        }.first()

    private class FakeParentalControlsRepository : ParentalControlsRepository {
        private val pinHashFlow = MutableStateFlow<String?>(null)
        private val restrictFlow = MutableStateFlow(false)

        override val pinHash: Flow<String?> = pinHashFlow
        override val restrictAdultFeatures: Flow<Boolean> = restrictFlow

        override suspend fun setPin(rawPin: String?) {
            val trimmed = rawPin?.trim()
            if (trimmed.isNullOrEmpty()) {
                pinHashFlow.value = null
                restrictFlow.value = false
            } else {
                pinHashFlow.value = "hash:$trimmed"
            }
        }

        override suspend fun verifyPin(rawPin: String): Boolean =
            pinHashFlow.value == "hash:${rawPin.trim()}"

        override suspend fun isPinSet(): Boolean = pinHashFlow.value != null

        override suspend fun setRestrictAdultFeatures(enabled: Boolean) {
            restrictFlow.value = enabled
        }

        fun restrictAdultFeaturesValue(): Boolean = restrictFlow.value
    }
}
