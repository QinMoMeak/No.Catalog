package com.nocatalog.app.domain.model

data class EntryFilter(
    val statuses: Set<EntryStatus> = emptySet(),
    val watched: Boolean? = null,
    val favorite: Boolean? = null,
    val minRating: Float? = null,
    val maxRating: Float? = null,
    val performer: String? = null,
    val tag: String? = null,
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

