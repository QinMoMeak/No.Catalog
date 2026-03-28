package com.nocatalog.app.presentation.ui.edit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.core.image.ImageStorageManager
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
    val coverLocalPath: String = "",
    val coverThumbPath: String = "",
    val coverRemoteUrl: String = "",
    val coverUpdatedAt: String = "",
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
    private val imageStorageManager: ImageStorageManager,
) : ViewModel() {

    private val mutableState = MutableStateFlow(EditUiState())
    val uiState: StateFlow<EditUiState> = mutableState.asStateFlow()

    private val entryIdArg: String? = savedStateHandle["entryId"]
    private val workingEntryId: String = entryIdArg
        ?.takeIf { it.isNotBlank() && it != "new" }
        ?: UUID.randomUUID().toString()

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
    fun onCoverLocalPathChange(value: String) = update { it.copy(coverLocalPath = value) }
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
                id = state.entryId ?: workingEntryId,
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
                coverLocalPath = state.coverLocalPath.ifBlank { null },
                coverThumbPath = state.coverThumbPath.ifBlank { null },
                coverRemoteUrl = state.coverRemoteUrl.ifBlank { null },
                coverUpdatedAt = state.coverUpdatedAt.ifBlank { null },
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

    fun importLocalCover(uri: Uri) {
        viewModelScope.launch {
            when (val result = imageStorageManager.importCoverFromUri(uri, uiState.value.entryId ?: workingEntryId)) {
                is com.nocatalog.app.core.common.AppResult.Error -> {
                    update { it.copy(errorMessage = result.error.toReadableMessage()) }
                }
                is com.nocatalog.app.core.common.AppResult.Success -> {
                    update {
                        it.copy(
                            entryId = it.entryId ?: workingEntryId,
                            coverLocalPath = result.data.localPath,
                            coverThumbPath = result.data.thumbPath,
                            coverUpdatedAt = DateTimeUtil.nowUtcIso(),
                            errorMessage = null,
                        )
                    }
                }
            }
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
                    coverLocalPath = entry.coverLocalPath.orEmpty(),
                    coverThumbPath = entry.coverThumbPath.orEmpty(),
                    coverRemoteUrl = entry.coverRemoteUrl.orEmpty(),
                    coverUpdatedAt = entry.coverUpdatedAt.orEmpty(),
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

    private fun com.nocatalog.app.core.common.AppError.toReadableMessage(): String {
        return when (this) {
            is com.nocatalog.app.core.common.AppError.Network -> message
            is com.nocatalog.app.core.common.AppError.Security -> message
            is com.nocatalog.app.core.common.AppError.Storage -> message
            is com.nocatalog.app.core.common.AppError.Validation -> message
            is com.nocatalog.app.core.common.AppError.Unknown -> message
        }
    }
}
