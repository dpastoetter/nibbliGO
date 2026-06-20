package com.nibbli.nibbligo.feature.pet.presentation

import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class PetStatePersistenceTest {

    @Test
    fun persist_writesRoomAndWidgetSnapshot() = runTest {
        var saved: PetState? = null
        val repo = object : PetRepository {
            override fun observePetState() = flowOf(PetState())
            override suspend fun getPetState() = PetState()
            override suspend fun savePetState(state: PetState) {
                saved = state
            }
        }
        val context = RuntimeEnvironment.getApplication()
        val persistence = PetStatePersistence(context, repo)
        val state = PetState(name = "nibbli-test")
        persistence.persist(state)
        assertEquals(state, saved)
    }
}
