package com.nocatalog.app.domain.usecase.entry

import com.nocatalog.app.domain.repository.EntryRepository
import javax.inject.Inject

class ObservePerformersUseCase @Inject constructor(
    private val repository: EntryRepository,
) {
    operator fun invoke() = repository.observePerformers()
}

class ObserveTagsUseCase @Inject constructor(
    private val repository: EntryRepository,
) {
    operator fun invoke() = repository.observeTags()
}

