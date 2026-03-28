package com.nocatalog.app.domain.usecase.entry

import com.nocatalog.app.domain.model.Entry
import com.nocatalog.app.domain.model.EntryFilter
import com.nocatalog.app.domain.model.EntrySort
import com.nocatalog.app.domain.repository.EntryRepository
import javax.inject.Inject

class AddEntryUseCase @Inject constructor(
    private val repository: EntryRepository,
) {
    suspend operator fun invoke(entry: Entry) = repository.addEntry(entry)
}

class UpdateEntryUseCase @Inject constructor(
    private val repository: EntryRepository,
) {
    suspend operator fun invoke(entry: Entry) = repository.updateEntry(entry)
}

class DeleteEntryUseCase @Inject constructor(
    private val repository: EntryRepository,
) {
    suspend operator fun invoke(id: String) = repository.deleteEntry(id)
}

class GetEntryDetailUseCase @Inject constructor(
    private val repository: EntryRepository,
) {
    suspend operator fun invoke(id: String) = repository.getEntry(id)
}

class SearchEntriesUseCase @Inject constructor(
    private val repository: EntryRepository,
) {
    suspend operator fun invoke(
        query: String,
        filter: EntryFilter?,
        sort: EntrySort,
    ) = repository.search(query, filter, sort)
}

