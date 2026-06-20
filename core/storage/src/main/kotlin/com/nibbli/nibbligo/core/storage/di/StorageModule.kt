package com.nibbli.nibbligo.core.storage.di

import android.content.Context
import androidx.room.Room
import com.nibbli.nibbligo.core.domain.repository.BenchmarkRepository
import com.nibbli.nibbligo.core.domain.repository.CompanionMemoryRepository
import com.nibbli.nibbligo.core.domain.repository.ChatRepository
import com.nibbli.nibbligo.core.domain.repository.ModelRepository
import com.nibbli.nibbligo.core.domain.repository.PetRepository
import com.nibbli.nibbligo.core.domain.repository.PromptRepository
import com.nibbli.nibbligo.core.domain.repository.RecordingRepository
import com.nibbli.nibbligo.core.domain.repository.ActionHistoryRepository
import com.nibbli.nibbligo.core.domain.repository.SkillPackageRepository
import com.nibbli.nibbligo.core.domain.repository.UserPreferencesRepository
import com.nibbli.nibbligo.core.storage.repository.ActionHistoryRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.SkillPackageRepositoryImpl
import com.nibbli.nibbligo.core.storage.local.NibbliDatabase
import com.nibbli.nibbligo.core.storage.repository.BenchmarkRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.CompanionMemoryRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.ChatRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.ModelRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.PetRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.PromptRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.RecordingRepositoryImpl
import com.nibbli.nibbligo.core.storage.repository.UserPreferencesRepositoryImpl
import com.nibbli.nibbligo.core.domain.download.HfModelDownloadScheduler
import com.nibbli.nibbligo.core.domain.model.InstalledModelPathResolver
import com.nibbli.nibbligo.core.storage.download.HfModelDownloadSchedulerImpl
import com.nibbli.nibbligo.core.storage.model.InstalledModelPathResolverImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): NibbliDatabase =
    Room.databaseBuilder(context, NibbliDatabase::class.java, "nibbli.db")
      .fallbackToDestructiveMigrationOnDowngrade()
      .build()

  @Provides fun providePetStateDao(db: NibbliDatabase) = db.petStateDao()
  @Provides fun provideCompanionMemoryFactDao(db: NibbliDatabase) = db.companionMemoryFactDao()
  @Provides fun provideModelInstallDao(db: NibbliDatabase) = db.modelInstallDao()
  @Provides fun provideConversationDao(db: NibbliDatabase) = db.conversationDao()
  @Provides fun provideMessageDao(db: NibbliDatabase) = db.messageDao()
  @Provides fun provideSavedPromptDao(db: NibbliDatabase) = db.savedPromptDao()
  @Provides fun provideBenchmarkRunDao(db: NibbliDatabase) = db.benchmarkRunDao()
  @Provides fun provideRecordingDao(db: NibbliDatabase) = db.recordingDao()
  @Provides fun provideActionHistoryDao(db: NibbliDatabase) = db.actionHistoryDao()
  @Provides fun provideSkillInstallDao(db: NibbliDatabase) = db.skillInstallDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds @Singleton abstract fun bindModelRepository(impl: ModelRepositoryImpl): ModelRepository
  @Binds @Singleton abstract fun bindPetRepository(impl: PetRepositoryImpl): PetRepository
  @Binds @Singleton abstract fun bindCompanionMemoryRepository(
    impl: CompanionMemoryRepositoryImpl,
  ): CompanionMemoryRepository
  @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
  @Binds @Singleton abstract fun bindPromptRepository(impl: PromptRepositoryImpl): PromptRepository
  @Binds @Singleton abstract fun bindBenchmarkRepository(impl: BenchmarkRepositoryImpl): BenchmarkRepository
  @Binds @Singleton abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository
  @Binds @Singleton abstract fun bindUserPreferences(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
  @Binds @Singleton abstract fun bindSkillPackageRepository(impl: SkillPackageRepositoryImpl): SkillPackageRepository
  @Binds @Singleton abstract fun bindActionHistoryRepository(impl: ActionHistoryRepositoryImpl): ActionHistoryRepository
  @Binds @Singleton abstract fun bindHfDownloadScheduler(impl: HfModelDownloadSchedulerImpl): HfModelDownloadScheduler
    @Binds @Singleton abstract fun bindInstalledModelPathResolver(
    impl: InstalledModelPathResolverImpl,
  ): InstalledModelPathResolver
  @Binds @Singleton abstract fun bindModelAvailabilityGate(
    impl: com.nibbli.nibbligo.core.storage.model.ModelAvailabilityGateImpl,
  ): com.nibbli.nibbligo.core.domain.model.ModelAvailabilityGate
}
