package com.nibbli.nibbligo.feature.pet.presentation

import android.content.Context
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.model.PetState
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetSnapshot
import com.nibbli.nibbligo.feature.pet.widget.PetWidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetStatePersistence @Inject constructor(
    @ApplicationContext private val context: Context,
    private val petRepository: PetRepository,
) {
    suspend fun persist(state: PetState) {
        petRepository.savePetState(state)
        PetWidgetSnapshot.write(context, state)
        PetWidgetUpdater.refresh(context)
    }
}
