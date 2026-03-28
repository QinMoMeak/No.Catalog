package com.nocatalog.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nocatalog.app.data.local.dao.EntryDao
import com.nocatalog.app.data.local.dao.EntryRelationDao
import com.nocatalog.app.data.local.dao.PerformerDao
import com.nocatalog.app.data.local.dao.TagDao
import com.nocatalog.app.data.local.entity.EntryEntity
import com.nocatalog.app.data.local.entity.EntryPerformerCrossRef
import com.nocatalog.app.data.local.entity.EntryTagCrossRef
import com.nocatalog.app.data.local.entity.PerformerEntity
import com.nocatalog.app.data.local.entity.TagEntity

@Database(
    entities = [
        EntryEntity::class,
        PerformerEntity::class,
        EntryPerformerCrossRef::class,
        TagEntity::class,
        EntryTagCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun performerDao(): PerformerDao
    abstract fun tagDao(): TagDao
    abstract fun entryRelationDao(): EntryRelationDao
}

