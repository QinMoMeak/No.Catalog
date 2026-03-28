package com.nocatalog.app.presentation.navigation

object Routes {
    const val lock = "lock"
    const val home = "home"
    const val stats = "stats"
    const val importPreview = "import_preview"
    const val backup = "backup"
    const val settings = "settings"
    const val detailPattern = "detail/{entryId}"
    const val editPattern = "edit/{entryId}"

    fun detail(entryId: String): String = "detail/$entryId"
    fun edit(entryId: String = "new"): String = "edit/$entryId"

    val topLevelRoutes = setOf(home, stats, settings)
}
