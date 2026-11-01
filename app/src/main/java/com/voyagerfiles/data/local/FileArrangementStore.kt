package com.voyagerfiles.data.local

import android.content.Context
import com.voyagerfiles.data.model.FileArrangement
import com.voyagerfiles.data.model.GroupBy
import com.voyagerfiles.data.model.SortBy
import com.voyagerfiles.data.model.SortOrder

/** Each persisted browser tab and split pane has its own stable key. No file paths or tokens are stored. */
class FileArrangementStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "finderdex_arrangement_v1", Context.MODE_PRIVATE,
    )

    fun read(sessionId: String): FileArrangement {
        val prefix = "session:$sessionId:"
        fun <T : Enum<T>> enumValue(raw: String?, fallback: T, all: Array<T>): T =
            all.firstOrNull { it.name == raw } ?: fallback
        return FileArrangement(
            sortBy = enumValue(prefs.getString(prefix + "by", null), SortBy.NAME, SortBy.entries.toTypedArray()),
            sortOrder = enumValue(prefs.getString(prefix + "direction", null), SortOrder.ASCENDING,
                SortOrder.entries.toTypedArray()),
            groupBy = enumValue(prefs.getString(prefix + "group", null), GroupBy.NONE, GroupBy.entries.toTypedArray()),
            groupOrder = enumValue(prefs.getString(prefix + "groupDirection", null),
                SortOrder.ASCENDING, SortOrder.entries.toTypedArray()),
            foldersFirst = prefs.getBoolean(prefix + "foldersFirst", true),
        )
    }

    fun write(sessionId: String, value: FileArrangement) {
        val prefix = "session:$sessionId:"
        prefs.edit()
            .putString(prefix + "by", value.sortBy.name)
            .putString(prefix + "direction", value.sortOrder.name)
            .putString(prefix + "group", value.groupBy.name)
            .putString(prefix + "groupDirection", value.groupOrder.name)
            .putBoolean(prefix + "foldersFirst", value.foldersFirst)
            .apply()
    }
}
