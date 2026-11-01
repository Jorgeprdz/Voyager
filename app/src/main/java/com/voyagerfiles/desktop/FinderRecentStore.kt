package com.voyagerfiles.desktop

import android.content.Context
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.FileSource
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** History of successful opens initiated by FinderDex, not filesystem mtime. Local-only. */
data class FinderRecentOpen(val path: String, val openedAt: Long)

internal object FinderRecentPolicy {
    fun bounded(
        values: List<FinderRecentOpen>, limit: Int = 100,
        readable: (String) -> Boolean = { File(it).let { file -> file.isFile && file.canRead() } },
    ): List<FinderRecentOpen> {
        require(limit in 1..500)
        return values.filter { it.path.startsWith("/") && readable(it.path) }
            .groupBy { it.path }.map { (_, duplicates) -> duplicates.maxBy { it.openedAt } }
            .sortedWith(compareByDescending<FinderRecentOpen> { it.openedAt }.thenBy { it.path })
            .take(limit)
    }
}

internal class FinderRecentStore(context: Context) {
    private val prefs = context.getSharedPreferences("finderdex_recent_opens", Context.MODE_PRIVATE)
    companion object { private val lock = Any() }
    fun list(): List<FinderRecentOpen> = synchronized(lock) {
        val arr = runCatching { JSONArray(prefs.getString("entries", "[]")) }.getOrDefault(JSONArray())
        val entries = (0 until arr.length()).mapNotNull { index ->
            runCatching {
                val o = arr.getJSONObject(index)
                FinderRecentOpen(o.getString("path"), o.getLong("at"))
            }.getOrNull()
        }
        FinderRecentPolicy.bounded(entries)
    }

    fun record(file: FileItem, openedAt: Long = System.currentTimeMillis()) {
        if (file.source != FileSource.LOCAL || file.isDirectory || openedAt <= 0) return
        synchronized(lock) {
            val entries = FinderRecentPolicy.bounded(
                listOf(FinderRecentOpen(file.path, openedAt)) + list()
            )
            persist(entries)
        }
    }

    fun remove(path: String) = synchronized(lock) { persist(list().filterNot { it.path == path }) }

    fun clear() = synchronized(lock) { prefs.edit().remove("entries").apply() }

    private fun persist(entries: List<FinderRecentOpen>) {
        val array = JSONArray()
        entries.forEach { array.put(JSONObject().put("path", it.path).put("at", it.openedAt)) }
        prefs.edit().putString("entries", array.toString()).apply()
    }
}
