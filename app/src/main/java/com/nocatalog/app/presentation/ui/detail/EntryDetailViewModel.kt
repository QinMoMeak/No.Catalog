package com.nocatalog.app.presentation.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.core.util.DateTimeUtil
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.usecase.entry.DeleteEntryUseCase
import com.nocatalog.app.domain.usecase.entry.GetEntryDetailUseCase
import com.nocatalog.app.domain.usecase.entry.UpdateEntryUseCase
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryDetailUiState(
    val isLoading: Boolean = true,
    val entry: Entry? = null,
    val deleted: Boolean = false,
    val errorMessage: String? = null,
) : UiState

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEntryDetailUseCase: GetEntryDetailUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = mutableState.asStateFlow()

    private val entryId: String = checkNotNull(savedStateHandle["entryId"])

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            val entry = getEntryDetailUseCase(entryId)
            mutableState.update {
                it.copy(
                    isLoading = false,
                    entry = entry,
                    errorMessage = if (entry == null) "记录不存在或已被删除" else null,
                )
            }
        }
    }

    fun toggleFavorite() {
        val entry = uiState.value.entry ?: return
        updateEntry(entry.copy(favorite = !entry.favorite))
    }

    fun toggleWatched() {
        val entry = uiState.value.entry ?: return
        updateEntry(entry.copy(watched = !entry.watched))
    }

    fun deleteEntry() {
        viewModelScope.launch {
            deleteEntryUseCase(entryId)
            mutableState.update { it.copy(deleted = true) }
        }
    }

    private fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            val updated = entry.copy(updatedAt = DateTimeUtil.nowUtcIso())
            updateEntryUseCase(updated)
            mutableState.update { it.copy(entry = updated) }
        }
    }
}
