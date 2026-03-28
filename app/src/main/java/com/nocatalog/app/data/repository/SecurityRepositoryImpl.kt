package com.nocatalog.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nocatalog.app.core.common.AppError
import com.nocatalog.app.core.common.AppResult
import com.nocatalog.app.core.security.CryptoManager
import com.nocatalog.app.core.security.PasswordManager
import com.nocatalog.app.domain.repository.SecurityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val passwordManager: PasswordManager,
    private val cryptoManager: CryptoManager,
) : SecurityRepository {

    override suspend fun isPasswordSet(): Boolean {
        val prefs = dataStore.data.first()
        return !prefs[Keys.passwordHash].isNullOrBlank() && !prefs[Keys.passwordSalt].isNullOrBlank()
    }

    override suspend fun setPassword(password: String): AppResult<Unit> {
        if (password.length < 4) {
            return AppResult.Error(AppError.Validation("密码长度至少 4 位"))
        }
        cryptoManager.ensureAppKey()
        val digest = passwordManager.hash(password)
        dataStore.edit {
            it[Keys.passwordHash] = digest.hash
            it[Keys.passwordSalt] = digest.salt
        }
        return AppResult.Success(Unit)
    }

    override suspend fun verifyPassword(password: String): Boolean {
        val prefs = dataStore.data.first()
        val hash = prefs[Keys.passwordHash] ?: return false
        val salt = prefs[Keys.passwordSalt] ?: return false
        return passwordManager.verify(password, salt, hash)
    }

    override suspend fun clearPassword(): AppResult<Unit> {
        dataStore.edit {
            it.remove(Keys.passwordHash)
            it.remove(Keys.passwordSalt)
        }
        return AppResult.Success(Unit)
    }

    private object Keys {
        val passwordHash = stringPreferencesKey("password_hash")
        val passwordSalt = stringPreferencesKey("password_salt")
    }
}
