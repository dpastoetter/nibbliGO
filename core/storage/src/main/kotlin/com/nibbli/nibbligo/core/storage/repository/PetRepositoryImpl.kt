package com.nibbli.nibbligo.core.storage.repository

import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.core.storage.local.dao.PetStateDao
import com.nibbli.nibbligo.core.storage.mapper.toDomain
import com.nibbli.nibbligo.core.storage.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetRepositoryImpl @Inject constructor(
  private val petStateDao: PetStateDao,
) : PetRepository {

  override fun observePetState(): Flow<PetState> =
    petStateDao.observe().map { entity ->
      entity?.toDomain() ?: PetState()
    }

  override suspend fun getPetState(): PetState =
    petStateDao.get()?.toDomain() ?: PetState()

  override suspend fun savePetState(state: PetState) {
    petStateDao.upsert(state.toEntity())
  }
}
