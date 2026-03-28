package com.nocatalog.app.data.mapper

import com.nocatalog.app.data.local.entity.EntryEntity
import com.nocatalog.app.data.local.entity.EntryWithRelations
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryStatus
import com.nocatalog.app.domain.model.Performer
import com.nocatalog.app.domain.model.Tag
import javax.inject.Inject

/**
 * 负责 Domain 与 Room 之间的结构转换。
 */
class EntryMapper @Inject constructor() {

    fun toDomain(source: EntryWithRelations): Entry {
        return Entry(
            id = source.entry.id,
            code = source.entry.code,
            title = source.entry.title,
            performers = source.performers.map { Performer(id = it.id, name = it.name) },
            tags = source.tags.map { Tag(id = it.id, name = it.name) },
            rating = source.entry.rating,
            notes = source.entry.notes,
            status = EntryStatus.valueOf(source.entry.status),
            favorite = source.entry.favorite,
            watched = source.entry.watched,
            releaseDate = source.entry.releaseDate,
            collectedAt = source.entry.collectedAt,
            sourceUrl = source.entry.sourceUrl,
            coverLocalPath = source.entry.coverLocalPath,
            coverThumbPath = source.entry.coverThumbPath,
            coverRemoteUrl = source.entry.coverRemoteUrl,
            coverUpdatedAt = source.entry.coverUpdatedAt,
            createdAt = source.entry.createdAt,
            updatedAt = source.entry.updatedAt,
        )
    }

    fun toEntity(entry: Entry): EntryEntity {
        return EntryEntity(
            id = entry.id,
            code = entry.code,
            title = entry.title,
            rating = entry.rating,
            notes = entry.notes,
            status = entry.status.name,
            favorite = entry.favorite,
            watched = entry.watched,
            releaseDate = entry.releaseDate,
            collectedAt = entry.collectedAt,
            sourceUrl = entry.sourceUrl,
            coverLocalPath = entry.coverLocalPath,
            coverThumbPath = entry.coverThumbPath,
            coverRemoteUrl = entry.coverRemoteUrl,
            coverUpdatedAt = entry.coverUpdatedAt,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt,
            deletedAt = null,
        )
    }
}
