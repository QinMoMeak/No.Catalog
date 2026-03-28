package com.nocatalog.app.core.csv

object CsvSchema {
    val headers = listOf(
        "id",
        "code",
        "title",
        "performers",
        "tags",
        "rating",
        "notes",
        "status",
        "favorite",
        "watched",
        "release_date",
        "collected_at",
        "source_url",
        "cover_local_path",
        "cover_remote_url",
        "created_at",
        "updated_at",
    )

    val aliases = mapOf(
        "performer_names" to "performers",
        "tag_names" to "tags",
        "releaseDate" to "release_date",
    )
}

