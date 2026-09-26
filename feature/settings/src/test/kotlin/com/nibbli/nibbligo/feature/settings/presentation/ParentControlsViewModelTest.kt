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
    fun removePin_requiresCurrentPin() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repo = FakeParentalControlsRepository()
        repo.setPin("1234")
        repo.setRestrictAdultFeatures(true)
        val viewModel = ParentControlsViewModel(repo)
        advanceUntilIdle()

        viewModel.removePin("9999")
        advanceUntilIdle()
        assertTrue(repo.isPinSet())
        assertEquals("Current PIN incorrect.", viewModel.uiState.value.message)

        viewModel.removePin("1234")
        advanceUntilIdle()

        assertFalse(repo.isPinSet())
        assertFalse(viewModel.uiState.value.pinSet)
        assertFalse(viewModel.uiState.value.restrictAdultFeatures)
    }

    @Test
    fun changePin_requiresCurrentPin() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val repo = FakeParentalControlsRepository()
        repo.setPin("1234")
        val viewModel = ParentControlsViewModel(repo)
        advanceUntilIdle()

        viewModel.setPin("5678")
        advanceUntilIdle()
        assertEquals("Enter the current PIN to change it.", viewModel.uiState.value.message)
        assertTrue(repo.verifyPin("1234"))

        viewModel.setPin("5678", currentPin = "1234")
        advanceUntilIdle()
        assertTrue(repo.verifyPin("5678"))
        assertEquals("Parent PIN saved.", viewModel.uiState.value.message)
    }

    @Test
    fun gateActive_onlyWhenRestrictAndPinSet() = runTest {
        val repo = FakeParentalControlsRepository()
        assertFalse(gateActive(repo))

        repo.setRestrictAdultFeatures(true)
        assertFalse(gateActive(repo))

        repo.setPin("1234")
        assertTrue(gateActive(repo))

        repo.clearPin("1234")
        assertFalse(gateActive(repo))
    }

    private suspend fun gateActive(repo: FakeParentalControlsRepository): Boolean =
        combine(repo.restrictAdultFeatures, repo.pinHash) { restrict, hash ->
            restrict && hash != null
        }.first()

    private class FakeParentalControlsRepository : ParentalControlsRepository {
        private val pinHashFlow = MutableStateFlow<String?>(null)
        private val restrictFlow = MutableStateFlow(false)
        private val lockedUntilFlow = MutableStateFlow(0L)

        override val pinHash: Flow<String?> = pinHashFlow
        override val restrictAdultFeatures: Flow<Boolean> = restrictFlow
        override val pinLockedUntilEpochMs: Flow<Long> = lockedUntilFlow

        override suspend fun setPin(rawPin: String, currentPin: String?): Boolean {
            val trimmed = rawPin.trim()
            if (trimmed.length < 4 || !trimmed.all { it.isDigit() }) return false
            if (pinHashFlow.value != null) {
                if (currentPin == null || !verifyPin(currentPin)) return false
            }
            pinHashFlow.value = "hash:$trimmed"
            return true
        }

        override suspend fun clearPin(currentPin: String): Boolean {
            if (pinHashFlow.value == null) return true
            if (!verifyPin(currentPin)) return false
            pinHashFlow.value = null
            restrictFlow.value = false
            return true
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
