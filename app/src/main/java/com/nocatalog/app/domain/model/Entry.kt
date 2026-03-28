package com.nocatalog.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Entry(
    val id: String,
    val code: String,
    val title: String,
    val performers: List<Performer>,
    val tags: List<Tag>,
    val rating: Float,
    val notes: String? = null,
    val status: EntryStatus = EntryStatus.COLLECTED,
    val favorite: Boolean = false,
    val watched: Boolean = false,
    val releaseDate: String? = null,
    val collectedAt: String,
    val sourceUrl: String? = null,
    val coverLocalPath: String? = null,
    val coverThumbPath: String? = null,
    val coverRemoteUrl: String? = null,
    val coverUpdatedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class Performer(
    val id: String,
    val name: String,
)

@Serializable
data class Tag(
    val id: String,
    val name: String,
)

@Serializable
enum class EntryStatus {
    WISH,
    COLLECTED,
    WATCHED,
    ARCHIVED,
}
