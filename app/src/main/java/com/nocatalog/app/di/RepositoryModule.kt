package com.nocatalog.app.di

import com.nocatalog.app.data.repository.BackupRepositoryImpl
import com.nocatalog.app.data.repository.EntryRepositoryImpl
import com.nocatalog.app.data.repository.ImportExportRepositoryImpl
import com.nocatalog.app.data.repository.SecurityRepositoryImpl
import com.nocatalog.app.data.repository.SettingsRepositoryImpl
import com.nocatalog.app.domain.repository.BackupRepository
import com.nocatalog.app.domain.repository.EntryRepository
import com.nocatalog.app.domain.repository.ImportExportRepository
import com.nocatalog.app.domain.repository.SecurityRepository
import com.nocatalog.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEntryRepository(impl: EntryRepositoryImpl): EntryRepository

    @Binds
    @Singleton
    abstract fun bindImportExportRepository(impl: ImportExportRepositoryImpl): ImportExportRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository
}

