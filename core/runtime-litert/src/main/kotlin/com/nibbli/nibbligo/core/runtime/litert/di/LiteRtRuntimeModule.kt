package com.nibbli.nibbligo.core.runtime.litert.di

import com.nibbli.nibbligo.core.runtime.RuntimePreference
import com.nibbli.nibbligo.core.runtime.litert.LiteRtRuntimePreference
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiteRtRuntimeModule {
    @Binds
    @Singleton
    abstract fun bindRuntimePreference(impl: LiteRtRuntimePreference): RuntimePreference
}
