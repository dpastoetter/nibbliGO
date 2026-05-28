package com.nibbli.nibbligo.di

import android.content.Context
import com.nibbli.nibbligo.BuildConfig
import com.nibbli.nibbligo.core.hf.download.HuggingFaceOAuthConfig
import com.nibbli.nibbligo.core.hf.download.HuggingFaceOAuthConfigLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppOAuthModule {
    @Provides
    @Singleton
    fun provideHuggingFaceOAuthConfig(
        @ApplicationContext context: Context,
    ): HuggingFaceOAuthConfig = HuggingFaceOAuthConfigLoader.load(
        context = context,
        buildClientId = BuildConfig.HF_OAUTH_CLIENT_ID,
        buildRedirectUri = BuildConfig.HF_OAUTH_REDIRECT_URI,
    )
}
