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
import com.nocatalog.app.data.local.entity.PerformerEntity
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
    @Query("SELECT * FROM performers WHERE normalized_name IN (:normalizedNames)")
    suspend fun getByNormalizedNames(normalizedNames: List<String>): List<PerformerEntity>

    @Upsert
    suspend fun upsertAll(performers: List<PerformerEntity>)
}

@Dao
interface TagDao {
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

