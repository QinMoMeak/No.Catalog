package com.nocatalog.app.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.model.HomeViewMode
import com.nocatalog.app.domain.repository.SecurityRepository
import com.nocatalog.app.domain.repository.SettingsRepository
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val homeViewMode: HomeViewMode = HomeViewMode.CARD,
    val defaultSort: EntrySort = EntrySort.UPDATED_DESC,
    val isPasswordSet: Boolean = false,
    val passwordInput: String = "",
    val message: String? = null,
) : UiState

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val securityRepository: SecurityRepository,
) : ViewModel() {

    private val passwordInput = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)
    private val isPasswordSet = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        passwordInput,
        message,
        isPasswordSet,
    ) { settings, password, tip, passwordSet ->
        SettingsUiState(
            homeViewMode = settings.homeViewMode,
            defaultSort = settings.defaultSort,
            isPasswordSet = passwordSet,
            passwordInput = password,
            message = tip,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    init {
        refreshSecurityState()
    }

    fun onPasswordInputChange(value: String) {
        passwordInput.update { value }
    }

    fun setCardMode() = updateHomeMode(HomeViewMode.CARD)

    fun setTableMode() = updateHomeMode(HomeViewMode.TABLE)

    fun cycleSort() {
        viewModelScope.launch {
            val next = when (uiState.value.defaultSort) {
                EntrySort.UPDATED_DESC -> EntrySort.CREATED_DESC
                EntrySort.CREATED_DESC -> EntrySort.RATING_DESC
                EntrySort.RATING_DESC -> EntrySort.TITLE_ASC
                EntrySort.TITLE_ASC -> EntrySort.CODE_ASC
                EntrySort.CODE_ASC -> EntrySort.RELEASE_DATE_DESC
                EntrySort.RELEASE_DATE_DESC -> EntrySort.UPDATED_DESC
            }
            settingsRepository.updateDefaultSort(next)
        }
    }

    fun savePassword() {
        viewModelScope.launch {
            val password = uiState.value.passwordInput
            if (password.length < 4) {
                message.update { "密码至少 4 位" }
                return@launch
            }
            securityRepository.setPassword(password)
            passwordInput.update { "" }
            message.update { "密码已保存，下次冷启动会要求解锁" }
            refreshSecurityState()
        }
    }

    fun clearPassword() {
        viewModelScope.launch {
            securityRepository.clearPassword()
            message.update { "密码锁已关闭" }
            refreshSecurityState()
        }
    }

    private fun updateHomeMode(mode: HomeViewMode) {
        viewModelScope.launch {
            settingsRepository.updateHomeViewMode(mode)
        }
    }

    private fun refreshSecurityState() {
        viewModelScope.launch {
            isPasswordSet.update { securityRepository.isPasswordSet() }
        }
    }
}

