package com.nocatalog.app.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryStatus
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.model.HomeViewMode
import com.nocatalog.app.domain.repository.EntryRepository
import com.nocatalog.app.domain.repository.SettingsRepository
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val query: String = "",
    val viewMode: HomeViewMode = HomeViewMode.CARD,
    val sort: EntrySort = EntrySort.UPDATED_DESC,
    val statusFilter: EntryStatus? = null,
    val favoriteOnly: Boolean = false,
    val watchedOnly: Boolean = false,
    val entries: List<Entry> = emptyList(),
) : UiState

@HiltViewModel
class HomeViewModel @Inject constructor(
    entryRepository: EntryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<EntryStatus?>(null)
    private val favoriteOnly = MutableStateFlow(false)
    private val watchedOnly = MutableStateFlow(false)
    private val filters = combine(
        query,
        statusFilter,
        favoriteOnly,
        watchedOnly,
    ) { search, status, favorite, watched ->
        FilterState(
            query = search,
            status = status,
            favoriteOnly = favorite,
            watchedOnly = watched,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        entryRepository.observeEntries(),
        settingsRepository.observeSettings(),
        filters,
    ) { entries, settings, filterState ->
        val filtered = if (filterState.query.isBlank()) {
            entries
        } else {
            entries.filter { entry ->
                listOf(
                    entry.code,
                    entry.title,
                    entry.notes.orEmpty(),
                    entry.performers.joinToString(" ") { it.name },
                    entry.tags.joinToString(" ") { it.name },
                ).any { it.contains(filterState.query, ignoreCase = true) }
            }
        }
            .filter { entry -> filterState.status == null || entry.status == filterState.status }
            .filter { entry -> !filterState.favoriteOnly || entry.favorite }
            .filter { entry -> !filterState.watchedOnly || entry.watched }
        HomeUiState(
            query = filterState.query,
            viewMode = settings.homeViewMode,
            sort = settings.defaultSort,
            statusFilter = filterState.status,
            favoriteOnly = filterState.favoriteOnly,
            watchedOnly = filterState.watchedOnly,
            entries = filtered.sortedWith(settings.defaultSort.comparator()),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onQueryChange(value: String) {
        query.update { value }
    }

    fun onToggleViewMode() {
        viewModelScope.launch {
            val next = if (uiState.value.viewMode == HomeViewMode.CARD) {
                HomeViewMode.TABLE
            } else {
                HomeViewMode.CARD
            }
            settingsRepository.updateHomeViewMode(next)
        }
    }

    fun onCycleSort() {
        viewModelScope.launch {
            val next = when (uiState.value.sort) {
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

    fun onToggleFavoriteOnly() {
        favoriteOnly.update { !it }
    }

    fun onToggleWatchedOnly() {
        watchedOnly.update { !it }
    }

    fun onCycleStatusFilter() {
        statusFilter.update { current ->
            when (current) {
                null -> EntryStatus.WISH
                EntryStatus.WISH -> EntryStatus.COLLECTED
                EntryStatus.COLLECTED -> EntryStatus.WATCHED
                EntryStatus.WATCHED -> EntryStatus.ARCHIVED
                EntryStatus.ARCHIVED -> null
            }
        }
    }

    private fun EntrySort.comparator(): Comparator<Entry> {
        return when (this) {
            EntrySort.UPDATED_DESC -> compareByDescending<Entry> { it.updatedAt }
            EntrySort.CREATED_DESC -> compareByDescending<Entry> { it.createdAt }
            EntrySort.RATING_DESC -> compareByDescending<Entry> { it.rating }
            EntrySort.TITLE_ASC -> compareBy<Entry> { it.title.lowercase() }
            EntrySort.CODE_ASC -> compareBy<Entry> { it.code.lowercase() }
            EntrySort.RELEASE_DATE_DESC -> compareByDescending<Entry> { it.releaseDate.orEmpty() }
        }
    }

    private data class FilterState(
        val query: String,
        val status: EntryStatus?,
        val favoriteOnly: Boolean,
        val watchedOnly: Boolean,
    )
}
