package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ParentalControlsRepositoryTest {

    private lateinit var repository: ParentalControlsRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = ParentalControlsRepositoryImpl(context)
        runBlocking { repository.setPin(null) }
    }

    @Test
    fun setPin_andVerify_succeedsWithCorrectPin() = runTest {
        repository.setPin("1234")
        assertTrue(repository.isPinSet())
        assertTrue(repository.verifyPin("1234"))
    }

    @Test
    fun verifyPin_failsWithWrongPin() = runTest {
        repository.setPin("1234")
        assertFalse(repository.verifyPin("9999"))
    }

    @Test
    fun verifyPin_trimsWhitespace() = runTest {
        repository.setPin("1234")
        assertTrue(repository.verifyPin("  1234  "))
    }

    @Test
    fun setPin_null_clearsPinAndRestrictFlag() = runTest {
        repository.setPin("1234")
        repository.setRestrictAdultFeatures(true)
        repository.setPin(null)
        assertFalse(repository.isPinSet())
        assertFalse(repository.restrictAdultFeatures.first())
    }

    @Test
    fun hashPin_isDeterministic() {
        assertEquals(hashPin("5678"), hashPin("5678"))
        assertFalse(hashPin("1234") == hashPin("5678"))
    }

    @Test
    fun verifyPin_returnsFalseWhenNoPinSet() = runTest {
        repository.setPin(null)
        assertFalse(repository.verifyPin("1234"))
    }
}
