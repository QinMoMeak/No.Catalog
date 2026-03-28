package com.nocatalog.app.presentation.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.core.security.AppLockManager
import com.nocatalog.app.domain.repository.SecurityRepository
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val password: String = "",
    val isPasswordSet: Boolean = true,
    val isUnlocked: Boolean = false,
    val errorMessage: String? = null,
) : UiState

@HiltViewModel
class LockViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val appLockManager: AppLockManager,
) : ViewModel() {

    private val mutableState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val passwordSet = securityRepository.isPasswordSet()
            if (!passwordSet) {
                appLockManager.markUnlocked()
            }
            mutableState.update {
                it.copy(isPasswordSet = passwordSet, isUnlocked = !passwordSet)
            }
        }
    }

    fun onPasswordChange(value: String) {
        mutableState.update { it.copy(password = value, errorMessage = null) }
    }

    fun unlock() {
        viewModelScope.launch {
            val ok = securityRepository.verifyPassword(mutableState.value.password)
            if (ok) {
                appLockManager.markUnlocked()
                mutableState.update { it.copy(isUnlocked = true, errorMessage = null) }
            } else {
                mutableState.update { it.copy(errorMessage = "密码错误，请重试") }
            }
        }
    }
}

