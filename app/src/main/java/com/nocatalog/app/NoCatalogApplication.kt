package com.nocatalog.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 应用入口，负责初始化全局依赖和日志能力。
 */
@HiltAndroidApp
class NoCatalogApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}

