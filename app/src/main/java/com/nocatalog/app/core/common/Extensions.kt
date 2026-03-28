package com.nocatalog.app.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

/**
 * 统一名称归一化规则，供演员、标签去重和搜索复用。
 */
fun String.normalizeToken(): String {
    return trim()
        .lowercase()
        .replace("\\s+".toRegex(), "")
}

fun String.escapeCsvCell(): String {
    val escaped = replace("\"", "\"\"")
    return "\"$escaped\""
}

