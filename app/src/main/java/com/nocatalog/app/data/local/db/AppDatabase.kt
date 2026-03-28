package com.nocatalog.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import com.nocatalog.app.data.local.dao.EntryDao
import com.nocatalog.app.data.local.dao.EntryRelationDao
import com.nocatalog.app.data.local.dao.PerformerDao
import com.nocatalog.app.data.local.dao.StatisticsDao
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
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun performerDao(): PerformerDao
    abstract fun tagDao(): TagDao
    abstract fun entryRelationDao(): EntryRelationDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN cover_thumb_path TEXT")
                db.execSQL("ALTER TABLE entries ADD COLUMN cover_updated_at TEXT")
            }
        }
    }
}
