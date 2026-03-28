package com.nocatalog.app.domain.repository

import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryFilter
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.model.Performer
import com.nocatalog.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    fun observeEntries(): Flow<List<Entry>>
    fun observePerformers(): Flow<List<Performer>>
    fun observeTags(): Flow<List<Tag>>
    suspend fun getEntry(id: String): Entry?
    suspend fun addEntry(entry: Entry)
    suspend fun updateEntry(entry: Entry)
    suspend fun deleteEntry(id: String)
    suspend fun search(query: String, filter: EntryFilter?, sort: EntrySort): List<Entry>
}

