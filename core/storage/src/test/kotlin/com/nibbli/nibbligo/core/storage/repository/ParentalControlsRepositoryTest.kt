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
        runBlocking { repository.clearAllForTests() }
    }

    @Test
    fun setPin_andVerify_succeedsWithCorrectPin() = runTest {
        assertTrue(repository.setPin("1234"))
        assertTrue(repository.isPinSet())
        assertTrue(repository.verifyPin("1234"))
    }

    @Test
    fun verifyPin_failsWithWrongPin() = runTest {
        assertTrue(repository.setPin("1234"))
        assertFalse(repository.verifyPin("9999"))
    }

    @Test
    fun verifyPin_trimsWhitespace() = runTest {
        assertTrue(repository.setPin("1234"))
        assertTrue(repository.verifyPin("  1234  "))
    }

    @Test
    fun clearPin_requiresCurrentPin() = runTest {
        assertTrue(repository.setPin("1234"))
        repository.setRestrictAdultFeatures(true)
        assertFalse(repository.clearPin("9999"))
        assertTrue(repository.isPinSet())
        assertTrue(repository.clearPin("1234"))
        assertFalse(repository.isPinSet())
        assertFalse(repository.restrictAdultFeatures.first())
    }

    @Test
    fun changePin_requiresCurrentPin() = runTest {
        assertTrue(repository.setPin("1234"))
        assertFalse(repository.setPin("5678", currentPin = null))
        assertFalse(repository.setPin("5678", currentPin = "0000"))
        assertTrue(repository.setPin("5678", currentPin = "1234"))
        assertTrue(repository.verifyPin("5678"))
        assertFalse(repository.verifyPin("1234"))
    }

    @Test
    fun hashPinPbkdf2_isDeterministicForSameSalt() {
        val salt = ByteArray(16) { 1 }
        assertEquals(hashPinPbkdf2("5678", salt), hashPinPbkdf2("5678", salt))
        assertFalse(hashPinPbkdf2("1234", salt) == hashPinPbkdf2("5678", salt))
    }

    @Test
    fun verifyPin_returnsFalseWhenNoPinSet() = runTest {
        assertTrue(repository.setPin("1234"))
        assertTrue(repository.clearPin("1234"))
        assertFalse(repository.verifyPin("1234"))
    }

    @Test
    fun verifyPin_locksAfterRepeatedFailures() = runTest {
        assertTrue(repository.setPin("1234"))
        repeat(5) { assertFalse(repository.verifyPin("0000")) }
        assertTrue(repository.pinLockedUntilEpochMs.first() > System.currentTimeMillis())
        assertFalse(repository.verifyPin("1234"))
    }
}
