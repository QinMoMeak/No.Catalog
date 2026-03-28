package com.nocatalog.app.data.repository

import androidx.room.withTransaction
import com.nocatalog.app.core.common.AppDispatchers
import com.nocatalog.app.core.common.normalizeToken
import com.nocatalog.app.core.util.DateTimeUtil
import com.nocatalog.app.data.local.dao.EntryDao
import com.nocatalog.app.data.local.dao.EntryRelationDao
import com.nocatalog.app.data.local.dao.PerformerDao
import com.nocatalog.app.data.local.dao.TagDao
import com.nocatalog.app.data.local.db.AppDatabase
import com.nocatalog.app.data.local.entity.EntryPerformerCrossRef
import com.nocatalog.app.data.local.entity.EntryTagCrossRef
import com.nocatalog.app.data.local.entity.PerformerEntity
import com.nocatalog.app.data.local.entity.TagEntity
import com.nocatalog.app.data.mapper.EntryMapper
import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryFilter
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.repository.EntryRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class EntryRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val entryDao: EntryDao,
    private val performerDao: PerformerDao,
    private val tagDao: TagDao,
    private val relationDao: EntryRelationDao,
    private val mapper: EntryMapper,
    private val dispatchers: AppDispatchers,
) : EntryRepository {

    override fun observeEntries(): Flow<List<Entry>> {
        return entryDao.observeAll()
            .map { entries -> entries.map(mapper::toDomain) }
            .flowOn(dispatchers.io)
    }

    override suspend fun getEntry(id: String): Entry? = withContext(dispatchers.io) {
        entryDao.getById(id)?.let(mapper::toDomain)
    }

    override suspend fun addEntry(entry: Entry) {
        upsertGraph(entry)
    }

    override suspend fun updateEntry(entry: Entry) {
        upsertGraph(entry)
    }

    override suspend fun deleteEntry(id: String) = withContext(dispatchers.io) {
        val now = DateTimeUtil.nowUtcIso()
        entryDao.softDelete(id = id, deletedAt = now, updatedAt = now)
    }

    override suspend fun search(
        query: String,
        filter: EntryFilter?,
        sort: EntrySort,
    ): List<Entry> = withContext(dispatchers.io) {
        val keyword = query.trim().lowercase()
        observeEntries().first()
            .filter { entry ->
                matchesQuery(entry, keyword) && matchesFilter(entry, filter)
            }
            .sortedWith(sort.comparator())
    }

    private suspend fun upsertGraph(entry: Entry) = withContext(dispatchers.io) {
        database.withTransaction {
            entryDao.insert(mapper.toEntity(entry))

            val performers = resolvePerformers(entry)
            val tags = resolveTags(entry)

            relationDao.deletePerformers(entry.id)
            relationDao.deleteTags(entry.id)
            relationDao.insertPerformers(
                performers.map { EntryPerformerCrossRef(entryId = entry.id, performerId = it.id) },
            )
            relationDao.insertTags(
                tags.map { EntryTagCrossRef(entryId = entry.id, tagId = it.id) },
            )
        }
    }

    private suspend fun resolvePerformers(entry: Entry): List<PerformerEntity> {
        val unique = entry.performers
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.normalizeToken() }
        if (unique.isEmpty()) return emptyList()

        val existing = performerDao.getByNormalizedNames(unique.map { it.name.normalizeToken() })
            .associateBy { it.normalizedName }
        val now = DateTimeUtil.nowUtcIso()
        val resolved = unique.map { performer ->
            val normalized = performer.name.normalizeToken()
            existing[normalized] ?: PerformerEntity(
                id = performer.id.ifBlank { UUID.randomUUID().toString() },
                name = performer.name.trim(),
                normalizedName = normalized,
                createdAt = now,
            )
        }
        performerDao.upsertAll(resolved)
        return resolved
    }

    private suspend fun resolveTags(entry: Entry): List<TagEntity> {
        val unique = entry.tags
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.normalizeToken() }
        if (unique.isEmpty()) return emptyList()

        val existing = tagDao.getByNormalizedNames(unique.map { it.name.normalizeToken() })
            .associateBy { it.normalizedName }
        val now = DateTimeUtil.nowUtcIso()
        val resolved = unique.map { tag ->
            val normalized = tag.name.normalizeToken()
            existing[normalized] ?: TagEntity(
                id = tag.id.ifBlank { UUID.randomUUID().toString() },
                name = tag.name.trim(),
                normalizedName = normalized,
                createdAt = now,
            )
        }
        tagDao.upsertAll(resolved)
        return resolved
    }

    private fun matchesQuery(entry: Entry, query: String): Boolean {
        if (query.isBlank()) return true
        return listOfNotNull(
            entry.code,
            entry.title,
            entry.notes.orEmpty(),
            entry.performers.joinToString(" ") { it.name },
            entry.tags.joinToString(" ") { it.name },
        ).any { it.lowercase().contains(query) }
    }

    private fun matchesFilter(entry: Entry, filter: EntryFilter?): Boolean {
        if (filter == null) return true
        if (filter.statuses.isNotEmpty() && entry.status !in filter.statuses) return false
        if (filter.watched != null && entry.watched != filter.watched) return false
        if (filter.favorite != null && entry.favorite != filter.favorite) return false
        if (filter.minRating != null && entry.rating < filter.minRating) return false
        if (filter.maxRating != null && entry.rating > filter.maxRating) return false
        if (!filter.performer.isNullOrBlank() && entry.performers.none { it.name.contains(filter.performer, true) }) return false
        if (!filter.tag.isNullOrBlank() && entry.tags.none { it.name.contains(filter.tag, true) }) return false
        return true
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
}

