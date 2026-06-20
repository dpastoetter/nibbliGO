package com.nibbli.nibbligo.core.storage.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nibbli.nibbligo.core.model.CompanionMemoryFactSource
import com.nibbli.nibbligo.core.model.CompanionMemoryLimits
import com.nibbli.nibbligo.core.storage.local.NibbliDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class CompanionMemoryRepositoryImplTest {

    private lateinit var db: NibbliDatabase
    private lateinit var repository: CompanionMemoryRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NibbliDatabase::class.java).build()
        repository = CompanionMemoryRepositoryImpl(db.companionMemoryFactDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun addFact_trimsToMaxFacts() = runTest {
        repeat(CompanionMemoryLimits.MAX_FACTS + 3) { index ->
            repository.addFact("fact-$index", CompanionMemoryFactSource.MANUAL)
        }
        val facts = repository.getFacts()
        assertEquals(CompanionMemoryLimits.MAX_FACTS, facts.size)
        assertTrue(facts.none { it.text == "fact-0" })
        assertTrue(facts.any { it.text == "fact-${CompanionMemoryLimits.MAX_FACTS + 2}" })
    }
}
