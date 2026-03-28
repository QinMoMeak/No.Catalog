package com.nocatalog.app.domain.repository

import com.nocatalog.app.core.common.AppResult

interface SecurityRepository {
    suspend fun isPasswordSet(): Boolean
    suspend fun setPassword(password: String): AppResult<Unit>
    suspend fun verifyPassword(password: String): Boolean
    suspend fun clearPassword(): AppResult<Unit>
}

