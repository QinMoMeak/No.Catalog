package com.nocatalog.app.domain.repository

import com.nocatalog.app.domain.model.StatisticsSummary
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeSummary(): Flow<StatisticsSummary>
}
