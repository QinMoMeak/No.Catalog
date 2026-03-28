package com.nocatalog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.nocatalog.app.data.local.entity.EntryEntity
import com.nocatalog.app.data.local.entity.EntryPerformerCrossRef
import com.nocatalog.app.data.local.entity.EntryTagCrossRef
import com.nocatalog.app.data.local.entity.EntryWithRelations
import com.nocatalog.app.data.local.entity.NameCountEntity
import com.nocatalog.app.data.local.entity.PerformerEntity
import com.nocatalog.app.data.local.entity.StatusCountEntity
import com.nocatalog.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Transaction
    @Query("SELECT * FROM entries WHERE deleted_at IS NULL ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<EntryWithRelations>>

    @Transaction
    @Query("SELECT * FROM entries WHERE deleted_at IS NULL ORDER BY updated_at DESC")
    suspend fun getAllWithRelations(): List<EntryWithRelations>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EntryWithRelations?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity)

    @Query("UPDATE entries SET deleted_at = :deletedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String, updatedAt: String)
}

@Dao
interface PerformerDao {
    @Query("SELECT * FROM performers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PerformerEntity>>

    @Query("SELECT * FROM performers WHERE normalized_name IN (:normalizedNames)")
    suspend fun getByNormalizedNames(normalizedNames: List<String>): List<PerformerEntity>

    @Upsert
    suspend fun upsertAll(performers: List<PerformerEntity>)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE normalized_name IN (:normalizedNames)")
    suspend fun getByNormalizedNames(normalizedNames: List<String>): List<TagEntity>

    @Upsert
    suspend fun upsertAll(tags: List<TagEntity>)
}

@Dao
interface EntryRelationDao {
    @Query("DELETE FROM entry_performer_ref WHERE entry_id = :entryId")
    suspend fun deletePerformers(entryId: String)

    @Query("DELETE FROM entry_tag_ref WHERE entry_id = :entryId")
    suspend fun deleteTags(entryId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformers(refs: List<EntryPerformerCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(refs: List<EntryTagCrossRef>)
}

@Dao
interface StatisticsDao {
    @Query("SELECT COUNT(*) FROM entries WHERE deleted_at IS NULL")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM entries WHERE deleted_at IS NULL AND watched = 1")
    suspend fun getWatchedCount(): Int

    @Query("SELECT COUNT(*) FROM entries WHERE deleted_at IS NULL AND favorite = 1")
    suspend fun getFavoriteCount(): Int

    @Query("SELECT COALESCE(AVG(rating), 0) FROM entries WHERE deleted_at IS NULL")
    suspend fun getAverageRating(): Float

    @Query(
        """
        SELECT status, COUNT(*) AS count
        FROM entries
        WHERE deleted_at IS NULL
        GROUP BY status
        ORDER BY count DESC
        """,
    )
    suspend fun getStatusCounts(): List<StatusCountEntity>

    @Query(
        """
        SELECT tags.id AS id, tags.name AS name, COUNT(entry_tag_ref.entry_id) AS count
        FROM tags
        INNER JOIN entry_tag_ref ON entry_tag_ref.tag_id = tags.id
        INNER JOIN entries ON entries.id = entry_tag_ref.entry_id
        WHERE entries.deleted_at IS NULL
        GROUP BY tags.id, tags.name
        ORDER BY count DESC, tags.name COLLATE NOCASE ASC
        LIMIT :limit
        """,
    )
    suspend fun getTopTags(limit: Int): List<NameCountEntity>

    @Query(
        """
        SELECT performers.id AS id, performers.name AS name, COUNT(entry_performer_ref.entry_id) AS count
        FROM performers
        INNER JOIN entry_performer_ref ON entry_performer_ref.performer_id = performers.id
        INNER JOIN entries ON entries.id = entry_performer_ref.entry_id
        WHERE entries.deleted_at IS NULL
        GROUP BY performers.id, performers.name
        ORDER BY count DESC, performers.name COLLATE NOCASE ASC
        LIMIT :limit
        """,
    )
    suspend fun getTopPerformers(limit: Int): List<NameCountEntity>

    @Query("SELECT COUNT(*) FROM entries WHERE deleted_at IS NULL AND created_at >= :sinceIso")
    suspend fun getAddedCountSince(sinceIso: String): Int
}
