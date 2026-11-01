package com.voyagerfiles.viewmodel

import android.content.Context
import com.voyagerfiles.ui.text.UiText
import com.voyagerfiles.ui.text.resolve
import org.json.JSONArray
import org.json.JSONObject

/**
 * Private, bounded operation journal. It stores status/progress, never source credentials
 * or an automatic replay instruction. An uncertain result after process death is FAILED
 * with an explicit warning, NOT silently resumed or counted as completed.
 */
interface TransferHistoryStore {
    fun restore(): List<OperationResult> = emptyList()
    fun started(progress: TransferProgress) {}
    fun updated(progress: TransferProgress) {}
    fun finished(history: List<OperationResult>) {}

    data object None : TransferHistoryStore
}

class AndroidTransferHistoryStore(context: Context) : TransferHistoryStore {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("finderdex_transfer_journal_v1", Context.MODE_PRIVATE)
    private var lastCheckpointNanos = 0L

    @Synchronized
    override fun restore(): List<OperationResult> {
        val history = runCatching { decodeHistory(prefs.getString(KEY_HISTORY, null)) }.getOrDefault(emptyList())
        val interrupted = runCatching {
            prefs.getString(KEY_ACTIVE, null)?.let { decodeProgress(JSONObject(it)) }
        }.getOrNull()
        if (interrupted == null) return history
        val label = (interrupted.label as? UiText.Dynamic)?.value.orEmpty()
        val uncertain = OperationResult(
            interrupted.copy(
                label = UiText.Dynamic("${label.ifBlank { "Operation" }} — interrupted; verify destination before retry"),
                currentItemName = null,
            ),
            OperationOutcome.FAILED,
        )
        val recovered = (listOf(uncertain) + history).take(MAX_HISTORY)
        // Remove the active marker atomically: never duplicate uncertain operations on next launch.
        runCatching {
            prefs.edit().putString(KEY_HISTORY, encodeHistory(recovered).toString()).remove(KEY_ACTIVE).commit()
        }
        return recovered
    }

    @Synchronized
    override fun started(progress: TransferProgress) {
        lastCheckpointNanos = System.nanoTime()
        runCatching { prefs.edit().putString(KEY_ACTIVE, encodeProgress(progress).toString()).commit() }
    }

    @Synchronized
    override fun updated(progress: TransferProgress) {
        val now = System.nanoTime()
        if (now - lastCheckpointNanos < 750_000_000L) return
        lastCheckpointNanos = now
        runCatching { prefs.edit().putString(KEY_ACTIVE, encodeProgress(progress).toString()).apply() }
    }

    @Synchronized
    override fun finished(history: List<OperationResult>) {
        runCatching {
            prefs.edit()
                .putString(KEY_HISTORY, encodeHistory(history.take(MAX_HISTORY)).toString())
                .remove(KEY_ACTIVE)
                .commit()
        }
    }

    private fun encodeProgress(progress: TransferProgress) = JSONObject().apply {
        put("label", runCatching { progress.label.resolve(appContext.resources) }.getOrElse { "Operation" })
        put("completedItems", progress.completedItems)
        put("totalItems", progress.totalItems ?: JSONObject.NULL)
        put("copiedBytes", progress.copiedBytes)
        put("totalBytes", progress.totalBytes ?: JSONObject.NULL)
        put("skippedItems", progress.skippedItems)
    }

    private fun decodeProgress(json: JSONObject) = TransferProgress(
        label = UiText.Dynamic(json.optString("label").ifBlank { "Operation" }),
        completedItems = json.optInt("completedItems", 0).coerceAtLeast(0),
        totalItems = json.optionalCount("totalItems"),
        copiedBytes = json.optLong("copiedBytes", 0L).coerceAtLeast(0L),
        totalBytes = json.optionalBytes("totalBytes"),
        skippedItems = json.optInt("skippedItems", 0).coerceAtLeast(0),
    )

    private fun JSONObject.optionalCount(key: String): Int? =
        if (isNull(key)) null else optInt(key, 0).coerceAtLeast(0)

    private fun JSONObject.optionalBytes(key: String): Long? =
        if (isNull(key)) null else optLong(key, 0L).coerceAtLeast(0L)

    private fun encodeHistory(history: List<OperationResult>) = JSONArray().apply {
        history.forEach { result ->
            put(JSONObject().apply {
                put("progress", encodeProgress(result.progress))
                put("outcome", result.outcome.name)
            })
        }
    }

    private fun decodeHistory(raw: String?): List<OperationResult> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        return (0 until minOf(array.length(), MAX_HISTORY)).mapNotNull { index ->
            runCatching {
                val entry = array.getJSONObject(index)
                OperationResult(
                    decodeProgress(entry.getJSONObject("progress")),
                    OperationOutcome.valueOf(entry.getString("outcome")),
                )
            }.getOrNull()
        }
    }

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_HISTORY = "history"
        const val MAX_HISTORY = 50
    }
}
