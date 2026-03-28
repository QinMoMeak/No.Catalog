package com.nocatalog.app.di

import android.content.Context
import androidx.room.Room
import com.nocatalog.app.core.common.Constants
import com.nocatalog.app.data.local.dao.EntryDao
import com.nocatalog.app.data.local.dao.EntryRelationDao
import com.nocatalog.app.data.local.dao.PerformerDao
import com.nocatalog.app.data.local.dao.TagDao
import com.nocatalog.app.data.local.db.AppDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME,
        ).build()
    }

    @Provides
    fun provideEntryDao(database: AppDatabase): EntryDao = database.entryDao()

    @Provides
    fun providePerformerDao(database: AppDatabase): PerformerDao = database.performerDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideEntryRelationDao(database: AppDatabase): EntryRelationDao = database.entryRelationDao()
}

