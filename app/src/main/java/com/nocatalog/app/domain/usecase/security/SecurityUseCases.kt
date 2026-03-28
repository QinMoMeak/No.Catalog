package com.nocatalog.app.domain.usecase.security

import com.nocatalog.app.domain.repository.SecurityRepository
import javax.inject.Inject

class SetPasswordUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(password: String) = repository.setPassword(password)
}

class VerifyPasswordUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke(password: String) = repository.verifyPassword(password)
}

class IsAppLockEnabledUseCase @Inject constructor(
    private val repository: SecurityRepository,
) {
    suspend operator fun invoke() = repository.isPasswordSet()
}
