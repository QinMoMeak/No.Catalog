package com.nocatalog.app.domain.repository

import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.model.HomeViewMode
import com.nocatalog.app.domain.model.UserSettings
import com.nocatalog.app.domain.model.WebDavConfig
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun updateHomeViewMode(mode: HomeViewMode)
    suspend fun updateDefaultSort(sort: EntrySort)
    suspend fun updateWebDavConfig(config: WebDavConfig)
}

