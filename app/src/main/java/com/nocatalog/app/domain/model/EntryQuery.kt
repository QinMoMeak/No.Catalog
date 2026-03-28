package com.nocatalog.app.domain.model

data class EntryFilter(
    val statuses: Set<EntryStatus> = emptySet(),
    val watched: Boolean? = null,
    val favorite: Boolean? = null,
    val performerIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    val minRating: Float? = null,
    val maxRating: Float? = null,
)

enum class EntrySort {
    UPDATED_DESC,
    CREATED_DESC,
    RATING_DESC,
    TITLE_ASC,
    CODE_ASC,
    RELEASE_DATE_DESC,
}

enum class HomeViewMode {
    CARD,
    TABLE,
}

data class UserSettings(
    val homeViewMode: HomeViewMode = HomeViewMode.CARD,
    val defaultSort: EntrySort = EntrySort.UPDATED_DESC,
    val csvEncoding: String = "UTF-8 with BOM",
    val webDavConfig: WebDavConfig? = null,
)

data class StatisticsSummary(
    val totalCount: Int,
    val watchedCount: Int,
    val unwatchedCount: Int,
    val favoriteCount: Int,
    val averageRating: Float,
    val statusCounts: List<StatusCount>,
    val topTags: List<NameCount>,
    val topPerformers: List<NameCount>,
    val addedIn7Days: Int,
    val addedIn30Days: Int,
)

data class StatusCount(
    val status: EntryStatus,
    val count: Int,
)

data class NameCount(
    val id: String,
    val name: String,
    val count: Int,
)
