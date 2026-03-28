package com.nocatalog.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.nocatalog.app.core.common.appDataStore
import com.nocatalog.app.core.security.AppLockManager
import com.nocatalog.app.core.security.CryptoManager
import com.nocatalog.app.core.security.PasswordManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.appDataStore
    }

    @Provides
    @Singleton
    fun providePasswordManager(): PasswordManager = PasswordManager()

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager = CryptoManager()

    @Provides
    @Singleton
    fun provideAppLockManager(): AppLockManager = AppLockManager()
}
