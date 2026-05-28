package com.nibbli.nibbligo.core.pet.llm.di

import com.nibbli.nibbligo.core.pet.llm.DelegatingPetReactionPort
import com.nibbli.nibbligo.core.pet.llm.PetReactionPort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PetLlmModule {
    @Binds
    @Singleton
    abstract fun bindPetReactionPort(impl: DelegatingPetReactionPort): PetReactionPort
}
