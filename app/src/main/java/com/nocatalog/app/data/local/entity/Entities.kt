package com.nocatalog.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "entries",
    indices = [
        Index("code"),
        Index("updated_at"),
        Index("deleted_at"),
    ],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val rating: Float,
    val notes: String?,
    val status: String,
    val favorite: Boolean,
    val watched: Boolean,
    @ColumnInfo(name = "release_date") val releaseDate: String?,
    @ColumnInfo(name = "collected_at") val collectedAt: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String?,
    @ColumnInfo(name = "cover_local_path") val coverLocalPath: String?,
    @ColumnInfo(name = "cover_thumb_path") val coverThumbPath: String?,
    @ColumnInfo(name = "cover_remote_url") val coverRemoteUrl: String?,
    @ColumnInfo(name = "cover_updated_at") val coverUpdatedAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String? = null,
)

data class NameCountEntity(
    val id: String,
    val name: String,
    val count: Int,
)

data class StatusCountEntity(
    val status: String,
    val count: Int,
)

@Entity(
    tableName = "performers",
    indices = [Index(value = ["normalized_name"], unique = true)],
)
data class PerformerEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["normalized_name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "normalized_name") val normalizedName: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
)

@Entity(
    tableName = "entry_performer_ref",
    primaryKeys = ["entry_id", "performer_id"],
    indices = [
        Index("performer_id"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PerformerEntity::class,
            parentColumns = ["id"],
            childColumns = ["performer_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EntryPerformerCrossRef(
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "performer_id") val performerId: String,
)

@Entity(
    tableName = "entry_tag_ref",
    primaryKeys = ["entry_id", "tag_id"],
    indices = [
        Index("tag_id"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class EntryTagCrossRef(
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
)

data class EntryWithRelations(
    @Embedded val entry: EntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryPerformerCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "performer_id",
        ),
    )
    val performers: List<PerformerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagCrossRef::class,
            parentColumn = "entry_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<TagEntity>,
)
