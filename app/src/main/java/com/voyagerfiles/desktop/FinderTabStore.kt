package com.voyagerfiles.desktop

import android.content.Context
import com.voyagerfiles.data.model.FileSource
import com.voyagerfiles.viewmodel.BrowserSession
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Only local metadata is restored. No remote credentials, grants or selections are serialized. */
internal class FinderTabStore(context: Context) {
    private val prefs = context.getSharedPreferences("finderdex_tabs", Context.MODE_PRIVATE)
    fun read(): Pair<List<BrowserSession>, String?> {
        val array = runCatching { JSONArray(prefs.getString("tabs", "[]")) }.getOrDefault(JSONArray())
        val sessions = (0 until minOf(array.length(), 12)).mapNotNull { index -> runCatching {
            val row = array.getJSONObject(index)
            val root = File(row.getString("root")).canonicalFile
            val path = File(row.getString("path")).canonicalFile
            require(path.toPath().startsWith(root.toPath()) && path.isDirectory && path.canRead())
            BrowserSession(row.getString("id"), row.getString("title"), FileSource.LOCAL, root.path, path.path)
        }.getOrNull() }.distinctBy { it.id }
        return sessions to prefs.getString("active", null)
    }
    fun write(sessions: List<BrowserSession>, active: String?) {
        val rows = JSONArray()
        sessions.filter { it.source == FileSource.LOCAL }.take(12).forEach { row ->
            rows.put(JSONObject().put("id", row.id).put("title", row.title).put("root", row.rootPath).put("path", row.currentPath))
        }
        prefs.edit().putString("tabs", rows.toString()).putString("active", active).apply()
    }
    fun scroll(session: String, path: String): Pair<Int, Int> =
        prefs.getInt("$session:$path:index", 0).coerceAtLeast(0) to prefs.getInt("$session:$path:offset", 0).coerceAtLeast(0)
    fun scroll(session: String, path: String, index: Int, offset: Int) {
        prefs.edit().putInt("$session:$path:index", index.coerceAtLeast(0))
            .putInt("$session:$path:offset", offset.coerceAtLeast(0)).apply()
    }
}
