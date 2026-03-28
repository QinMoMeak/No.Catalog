package com.nocatalog.app.di

import com.nocatalog.app.core.image.DefaultImageStorageManager
import com.nocatalog.app.core.image.ImageStorageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageModule {

    @Binds
    @Singleton
    abstract fun bindImageStorageManager(impl: DefaultImageStorageManager): ImageStorageManager
}
