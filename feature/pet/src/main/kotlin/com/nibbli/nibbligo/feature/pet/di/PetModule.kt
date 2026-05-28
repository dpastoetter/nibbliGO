package com.nibbli.nibbligo.feature.pet.di

import com.nibbli.nibbligo.feature.pet.domain.PetSimulationEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PetModule {
    @Provides
    @Singleton
    fun providePetSimulationEngine(): PetSimulationEngine = PetSimulationEngine()
}
