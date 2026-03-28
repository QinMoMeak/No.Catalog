package com.nocatalog.app.domain.usecase.statistics

import com.nocatalog.app.domain.repository.StatisticsRepository
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val repository: StatisticsRepository,
) {
    operator fun invoke() = repository.observeSummary()
}
