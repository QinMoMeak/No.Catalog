package com.nocatalog.app.presentation.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nocatalog.app.domain.model.EntryStatus
import com.nocatalog.app.domain.model.StatisticsSummary
import com.nocatalog.app.domain.usecase.statistics.GetStatisticsUseCase
import com.nocatalog.app.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NameCountUi(
    val id: String,
    val name: String,
    val count: Int,
)

data class StatusCountUi(
    val status: EntryStatus,
    val count: Int,
)

data class StatsUiState(
    val totalCount: Int = 0,
    val watchedCount: Int = 0,
    val unwatchedCount: Int = 0,
    val favoriteCount: Int = 0,
    val averageRating: Float = 0f,
    val statusCounts: List<StatusCountUi> = emptyList(),
    val topTags: List<NameCountUi> = emptyList(),
    val topPerformers: List<NameCountUi> = emptyList(),
    val addedIn7Days: Int = 0,
    val addedIn30Days: Int = 0,
) : UiState

@HiltViewModel
class StatsViewModel @Inject constructor(
    getStatisticsUseCase: GetStatisticsUseCase,
) : ViewModel() {
    val uiState: StateFlow<StatsUiState> = getStatisticsUseCase()
        .map(::toStatsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
        )

    private fun toStatsUiState(summary: StatisticsSummary): StatsUiState {
        return StatsUiState(
            totalCount = summary.totalCount,
            watchedCount = summary.watchedCount,
            unwatchedCount = summary.unwatchedCount,
            favoriteCount = summary.favoriteCount,
            averageRating = summary.averageRating,
            statusCounts = summary.statusCounts.map { StatusCountUi(status = it.status, count = it.count) },
            topTags = summary.topTags.map { NameCountUi(id = it.id, name = it.name, count = it.count) },
            topPerformers = summary.topPerformers.map { NameCountUi(id = it.id, name = it.name, count = it.count) },
            addedIn7Days = summary.addedIn7Days,
            addedIn30Days = summary.addedIn30Days,
        )
    }
}
