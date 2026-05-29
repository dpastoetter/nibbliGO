package com.nibbli.nibbligo.di

import com.nibbli.nibbligo.core.runtime.InferenceRuntime
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceRuntimeModule {
    @Binds
    @Singleton
    abstract fun bindInferenceRuntime(impl: com.nibbli.nibbligo.core.runtime.litert.LiteRTInferenceRuntime): InferenceRuntime
}
