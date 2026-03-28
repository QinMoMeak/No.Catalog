package com.nocatalog.app.data.repository

import com.nocatalog.app.core.common.AppDispatchers
import com.nocatalog.app.core.util.DateTimeUtil
import com.nocatalog.app.data.local.dao.EntryDao
import com.nocatalog.app.data.local.dao.StatisticsDao
import com.nocatalog.app.domain.model.EntryStatus
import com.nocatalog.app.domain.model.NameCount
import com.nocatalog.app.domain.model.StatisticsSummary
import com.nocatalog.app.domain.model.StatusCount
import com.nocatalog.app.domain.repository.StatisticsRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

/**
 * 基于 Room 聚合查询构建统计汇总。
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsRepositoryImpl @Inject constructor(
    private val entryDao: EntryDao,
    private val statisticsDao: StatisticsDao,
    private val dispatchers: AppDispatchers,
) : StatisticsRepository {

    override fun observeSummary(): Flow<StatisticsSummary> {
        return entryDao.observeAll()
            .mapLatest {
                val totalCount = statisticsDao.getTotalCount()
                val watchedCount = statisticsDao.getWatchedCount()
                val favoriteCount = statisticsDao.getFavoriteCount()
                StatisticsSummary(
                    totalCount = totalCount,
                    watchedCount = watchedCount,
                    unwatchedCount = totalCount - watchedCount,
                    favoriteCount = favoriteCount,
                    averageRating = statisticsDao.getAverageRating(),
                    statusCounts = statisticsDao.getStatusCounts().map {
                        StatusCount(
                            status = runCatching { EntryStatus.valueOf(it.status) }.getOrDefault(EntryStatus.COLLECTED),
                            count = it.count,
                        )
                    },
                    topTags = statisticsDao.getTopTags(limit = 8).map { NameCount(it.id, it.name, it.count) },
                    topPerformers = statisticsDao.getTopPerformers(limit = 8).map { NameCount(it.id, it.name, it.count) },
                    addedIn7Days = statisticsDao.getAddedCountSince(daysAgoIso(7)),
                    addedIn30Days = statisticsDao.getAddedCountSince(daysAgoIso(30)),
                )
            }
            .flowOn(dispatchers.io)
    }

    private fun daysAgoIso(days: Long): String {
        return DateTimeUtil.run {
            java.time.format.DateTimeFormatter.ISO_INSTANT.format(
                Instant.now().minus(days, ChronoUnit.DAYS),
            )
        }
    }
}
