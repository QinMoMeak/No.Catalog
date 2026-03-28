package com.nocatalog.app.presentation.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.core.util.DateTimeUtil
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryStatus
import com.nocatalog.app.domain.model.Performer
import com.nocatalog.app.domain.model.Tag
import com.nocatalog.app.domain.usecase.entry.AddEntryUseCase
import com.nocatalog.app.domain.usecase.entry.GetEntryDetailUseCase
import com.nocatalog.app.domain.usecase.entry.UpdateEntryUseCase
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditUiState(
    val entryId: String? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val code: String = "",
    val title: String = "",
    val performersText: String = "",
    val tagsText: String = "",
    val rating: Float = 0f,
    val notes: String = "",
    val status: EntryStatus = EntryStatus.COLLECTED,
    val watched: Boolean = false,
    val favorite: Boolean = false,
    val releaseDate: String = "",
    val sourceUrl: String = "",
    val coverRemoteUrl: String = "",
    val collectedAt: String = "",
    val createdAt: String = "",
    val errorMessage: String? = null,
    val saveCompleted: Boolean = false,
) : UiState

@HiltViewModel
class EditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addEntryUseCase: AddEntryUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val getEntryDetailUseCase: GetEntryDetailUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = mutableState.asStateFlow()

    private val entryIdArg: String? = savedStateHandle["entryId"]

    init {
        if (!entryIdArg.isNullOrBlank() && entryIdArg != "new") {
            loadEntry(entryIdArg)
        }
    }

    fun onCodeChange(value: String) = update { it.copy(code = value.uppercase(), errorMessage = null) }
    fun onTitleChange(value: String) = update { it.copy(title = value, errorMessage = null) }
    fun onPerformersChange(value: String) = update { it.copy(performersText = value) }
    fun onTagsChange(value: String) = update { it.copy(tagsText = value) }
    fun onRatingChange(value: Float) = update { it.copy(rating = value) }
    fun onNotesChange(value: String) = update { it.copy(notes = value) }
    fun onReleaseDateChange(value: String) = update { it.copy(releaseDate = value) }
    fun onSourceUrlChange(value: String) = update { it.copy(sourceUrl = value) }
    fun onCoverRemoteUrlChange(value: String) = update { it.copy(coverRemoteUrl = value) }
    fun onToggleWatched() = update { it.copy(watched = !it.watched) }
    fun onToggleFavorite() = update { it.copy(favorite = !it.favorite) }
    fun onCycleStatus() = update {
        it.copy(
            status = when (it.status) {
                EntryStatus.WISH -> EntryStatus.COLLECTED
                EntryStatus.COLLECTED -> EntryStatus.WATCHED
                EntryStatus.WATCHED -> EntryStatus.ARCHIVED
                EntryStatus.ARCHIVED -> EntryStatus.WISH
            },
        )
    }

    fun save() {
        val state = mutableState.value
        if (state.code.isBlank() || state.title.isBlank()) {
            update { it.copy(errorMessage = "番号和标题不能为空") }
            return
        }

        viewModelScope.launch {
            val now = DateTimeUtil.nowUtcIso()
            val entry = Entry(
                id = state.entryId ?: UUID.randomUUID().toString(),
                code = state.code,
                title = state.title,
                performers = state.performersText.splitInput().map {
                    Performer(id = UUID.randomUUID().toString(), name = it)
                },
                tags = state.tagsText.splitInput().map {
                    Tag(id = UUID.randomUUID().toString(), name = it)
                },
                rating = state.rating,
                notes = state.notes.ifBlank { null },
                status = state.status,
                favorite = state.favorite,
                watched = state.watched,
                releaseDate = state.releaseDate.ifBlank { null },
                collectedAt = state.collectedAt.ifBlank { now },
                sourceUrl = state.sourceUrl.ifBlank { null },
                coverLocalPath = null,
                coverRemoteUrl = state.coverRemoteUrl.ifBlank { null },
                createdAt = state.createdAt.ifBlank { now },
                updatedAt = now,
            )
            if (state.isEditing) {
                updateEntryUseCase(entry)
            } else {
                addEntryUseCase(entry)
            }
            update { it.copy(saveCompleted = true) }
        }
    }

    private fun loadEntry(entryId: String) {
        viewModelScope.launch {
            update { it.copy(isLoading = true, errorMessage = null) }
            val entry = getEntryDetailUseCase(entryId)
            if (entry == null) {
                update { it.copy(isLoading = false, errorMessage = "未找到要编辑的记录") }
                return@launch
            }
            update {
                it.copy(
                    entryId = entry.id,
                    isEditing = true,
                    isLoading = false,
                    code = entry.code,
                    title = entry.title,
                    performersText = entry.performers.joinToString(", ") { performer -> performer.name },
                    tagsText = entry.tags.joinToString(", ") { tag -> tag.name },
                    rating = entry.rating,
                    notes = entry.notes.orEmpty(),
                    status = entry.status,
                    watched = entry.watched,
                    favorite = entry.favorite,
                    releaseDate = entry.releaseDate.orEmpty(),
                    sourceUrl = entry.sourceUrl.orEmpty(),
                    coverRemoteUrl = entry.coverRemoteUrl.orEmpty(),
                    collectedAt = entry.collectedAt,
                    createdAt = entry.createdAt,
                )
            }
        }
    }

    private fun update(transform: (EditUiState) -> EditUiState) {
        mutableState.update(transform)
    }

    private fun String.splitInput(): List<String> {
        return split(",", "，", "|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
