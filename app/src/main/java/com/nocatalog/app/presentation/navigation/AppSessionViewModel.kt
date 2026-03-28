package com.nocatalog.app.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.core.security.AppLockManager
import com.nocatalog.app.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppSessionUiState(
    val initialized: Boolean = false,
    val requiresLock: Boolean = true,
)

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val appLockManager: AppLockManager,
    private val securityRepository: SecurityRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppSessionUiState())
    val uiState: StateFlow<AppSessionUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            appLockManager.observeUnlocked().collect { unlocked ->
                val passwordSet = securityRepository.isPasswordSet()
                mutableState.update {
                    it.copy(
                        initialized = true,
                        requiresLock = passwordSet && !unlocked,
                    )
                }
            }
        }
    }
}
