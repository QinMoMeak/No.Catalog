package com.nocatalog.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.model.HomeViewMode
import com.nocatalog.app.domain.model.UserSettings
import com.nocatalog.app.domain.model.WebDavConfig
import com.nocatalog.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> {
        return dataStore.data.map { preferences ->
            val baseUrl = preferences[Keys.webDavBaseUrl]
            val username = preferences[Keys.webDavUsername]
            val password = preferences[Keys.webDavPassword]
            val remoteDir = preferences[Keys.webDavRemoteDir]
            UserSettings(
                homeViewMode = preferences[Keys.homeViewMode]
                    ?.let(HomeViewMode::valueOf)
                    ?: HomeViewMode.CARD,
                defaultSort = preferences[Keys.defaultSort]
                    ?.let(EntrySort::valueOf)
                    ?: EntrySort.UPDATED_DESC,
                webDavConfig = if (
                    baseUrl.isNullOrBlank() ||
                    username.isNullOrBlank() ||
                    password.isNullOrBlank() ||
                    remoteDir.isNullOrBlank()
                ) {
                    null
                } else {
                    WebDavConfig(baseUrl, username, password, remoteDir)
                },
            )
        }
    }

    override suspend fun updateHomeViewMode(mode: HomeViewMode) {
        dataStore.edit { it[Keys.homeViewMode] = mode.name }
    }

    override suspend fun updateDefaultSort(sort: EntrySort) {
        dataStore.edit { it[Keys.defaultSort] = sort.name }
    }

    override suspend fun updateWebDavConfig(config: WebDavConfig) {
        dataStore.edit {
            it[Keys.webDavBaseUrl] = config.baseUrl
            it[Keys.webDavUsername] = config.username
            it[Keys.webDavPassword] = config.password
            it[Keys.webDavRemoteDir] = config.remoteDir
        }
    }

    private object Keys {
        val homeViewMode = stringPreferencesKey("home_view_mode")
        val defaultSort = stringPreferencesKey("default_sort")
        val webDavBaseUrl = stringPreferencesKey("webdav_base_url")
        val webDavUsername = stringPreferencesKey("webdav_username")
        val webDavPassword = stringPreferencesKey("webdav_password")
        val webDavRemoteDir = stringPreferencesKey("webdav_remote_dir")
    }
}

