package com.voyagerfiles.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voyagerfiles.data.index.CategoryIndexState
import com.voyagerfiles.data.index.CategoryScanStatus
import com.voyagerfiles.data.index.StorageCategoryIndex
import com.voyagerfiles.data.index.StorageSearchFilters
import com.voyagerfiles.data.model.FileTypeFilter
import org.json.JSONArray
import org.json.JSONObject
import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.util.FileUtils
import com.voyagerfiles.util.StorageVolumeInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SavedStorageSearch(
    val title: String,
    val query: String,
    val filters: StorageSearchFilters,
)

data class StorageSearchState(
    val query: String = "",
    val submittedQuery: String = "",
    val result: CategoryIndexState = CategoryIndexState(),
    val filters: StorageSearchFilters = StorageSearchFilters(),
    val submittedFilters: StorageSearchFilters = StorageSearchFilters(),
    val currentFolder: String? = null,
    val minimumMegabytes: String = "",
    val maximumMegabytes: String = "",
    val modifiedWithinDays: Int? = null,
    val savedSearches: List<SavedStorageSearch> = emptyList(),
)

class StorageSearchViewModel @JvmOverloads constructor(
    application: Application,
    private val index: StorageCategoryIndex = StorageCategoryIndex(),
    private val volumeProvider: () -> List<StorageVolumeInfo> = { FileUtils.getStorageVolumes(application) },
    private val permissionGranted: () -> Boolean = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(application, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    },
) : AndroidViewModel(application) {
    private val savedPrefs = application.getSharedPreferences("finderdex_saved_searches_v1", Application.MODE_PRIVATE)
    private val _state = MutableStateFlow(StorageSearchState(savedSearches = readSavedSearches()))
    val state = _state.asStateFlow()
    private var scanJob: Job? = null
    private var generation = 0L

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun setCurrentFolder(path: String?) {
        _state.update { it.copy(currentFolder = path) }
    }

    fun setScopeToCurrentFolder(enabled: Boolean) {
        _state.update { state ->
            state.copy(filters = state.filters.copy(scopePath = if (enabled) state.currentFolder else null))
        }
    }

    fun setIncludeSubfolders(enabled: Boolean) {
        _state.update { it.copy(filters = it.filters.copy(includeSubfolders = enabled)) }
    }

    fun setFileType(type: FileTypeFilter) {
        _state.update { it.copy(filters = it.filters.copy(type = type)) }
    }

    fun setMinimumMegabytes(text: String) {
        _state.update { it.copy(minimumMegabytes = text.filter(Char::isDigit).take(9)) }
    }

    fun setMaximumMegabytes(text: String) {
        _state.update { it.copy(maximumMegabytes = text.filter(Char::isDigit).take(9)) }
    }

    fun setModifiedWithinDays(days: Int?) {
        _state.update { it.copy(modifiedWithinDays = days?.takeIf { value -> value > 0 }) }
    }

    private fun activeFilters(state: StorageSearchState): StorageSearchFilters = state.filters.copy(
        minSizeBytes = state.minimumMegabytes.toLongOrNull()?.times(1_048_576L),
        maxSizeBytes = state.maximumMegabytes.toLongOrNull()?.times(1_048_576L),
        modifiedAfterMillis = state.modifiedWithinDays?.let { days ->
            System.currentTimeMillis() - days * 86_400_000L
        },
    )

    fun saveCurrentSearch() {
        val state = _state.value
        val query = state.query.trim()
        if (query.isBlank()) return
        val saved = SavedStorageSearch(query, query, activeFilters(state))
        val items = (listOf(saved) + state.savedSearches.filterNot { it.title.equals(query, ignoreCase = true) }).take(12)
        _state.update { it.copy(savedSearches = items) }
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("title", item.title)
                put("query", item.query)
                put("type", item.filters.type.name)
                put("scope", item.filters.scopePath ?: JSONObject.NULL)
                put("subfolders", item.filters.includeSubfolders)
                put("minimum", item.filters.minSizeBytes ?: JSONObject.NULL)
                put("maximum", item.filters.maxSizeBytes ?: JSONObject.NULL)
                put("after", item.filters.modifiedAfterMillis ?: JSONObject.NULL)
            })
        }
        savedPrefs.edit().putString("saved", array.toString()).apply()
    }

    fun loadSavedSearch(index: Int) {
        val saved = _state.value.savedSearches.getOrNull(index) ?: return
        _state.update {
            it.copy(
                query = saved.query,
                filters = saved.filters,
                minimumMegabytes = saved.filters.minSizeBytes?.div(1_048_576L)?.toString().orEmpty(),
                maximumMegabytes = saved.filters.maxSizeBytes?.div(1_048_576L)?.toString().orEmpty(),
                modifiedWithinDays = null,
            )
        }
    }

    private fun readSavedSearches(): List<SavedStorageSearch> = runCatching {
        val array = JSONArray(savedPrefs.getString("saved", "[]"))
        (0 until minOf(array.length(), 12)).mapNotNull { index ->
            runCatching {
                val obj = array.getJSONObject(index)
                SavedStorageSearch(
                    obj.getString("title"),
                    obj.getString("query"),
                    StorageSearchFilters(
                        type = FileTypeFilter.valueOf(obj.getString("type")),
                        scopePath = obj.optString("scope").takeUnless { it.isBlank() || it == "null" },
                        includeSubfolders = obj.optBoolean("subfolders", true),
                        minSizeBytes = obj.optLong("minimum").takeIf { !obj.isNull("minimum") },
                        maxSizeBytes = obj.optLong("maximum").takeIf { !obj.isNull("maximum") },
                        modifiedAfterMillis = obj.optLong("after").takeIf { !obj.isNull("after") },
                    ),
                )
            }.getOrNull()
        }
    }.getOrDefault(emptyList())

    fun submit(showHidden: Boolean, accessGranted: Boolean) {
        val state = _state.value
        start(state.query.trim(), showHidden, accessGranted, activeFilters(state))
    }

    fun refresh(showHidden: Boolean, accessGranted: Boolean) {
        val state = _state.value
        start(state.submittedQuery, showHidden, accessGranted, state.submittedFilters)
    }

    private fun start(query: String, showHidden: Boolean, accessGranted: Boolean, filters: StorageSearchFilters) {
        val request = ++generation
        scanJob?.cancel()
        if (!accessGranted || !permissionGranted()) {
            _state.update { it.copy(submittedQuery = query, submittedFilters = filters, result = CategoryIndexState(status = CategoryScanStatus.DENIED)) }
            return
        }
        if (query.isBlank()) {
            _state.update { it.copy(submittedQuery = "", submittedFilters = filters, result = CategoryIndexState()) }
            return
        }
        _state.update { it.copy(submittedQuery = query, submittedFilters = filters, result = CategoryIndexState(status = CategoryScanStatus.SCANNING)) }
        scanJob = viewModelScope.launch {
            try {
                val volumes = withContext(Dispatchers.IO) { volumeProvider() }
                index.search(query, volumes, showHidden, filters, permissionGranted).collect { result ->
                    if (generation == request) _state.update { it.copy(result = result) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == request) {
                    _state.update {
                        it.copy(result = if (permissionGranted()) it.result.copy(status = CategoryScanStatus.FAILED)
                            else CategoryIndexState(status = CategoryScanStatus.DENIED))
                    }
                }
            }
        }
    }

    fun cancel() {
        generation++
        scanJob?.cancel()
        _state.update { state ->
            if (state.result.status != CategoryScanStatus.SCANNING) state
            else state.copy(result = state.result.copy(
                status = CategoryScanStatus.CANCELLED,
                coverage = state.result.coverage.map {
                    if (it.status == CategoryScanStatus.SCANNING || it.status == CategoryScanStatus.PENDING) {
                        it.copy(status = CategoryScanStatus.CANCELLED)
                    } else it
                },
            ))
        }
    }

    suspend fun validate(file: FileItem): Boolean {
        if (!permissionGranted()) {
            cancel()
            _state.update { it.copy(result = CategoryIndexState(status = CategoryScanStatus.DENIED)) }
            return false
        }
        val request = generation
        if (_state.value.result.files.none { it.path == file.path }) return false
        val current = withContext(Dispatchers.IO) {
            runCatching { index.isCurrent(file, volumeProvider(), permissionGranted) }.getOrDefault(false)
        }
        if (request != generation) return false
        if (!current) {
            cancel()
            _state.update { state ->
                state.copy(result = if (!permissionGranted()) CategoryIndexState(status = CategoryScanStatus.DENIED)
                    else state.result.copy(
                        status = CategoryScanStatus.PARTIAL,
                        files = state.result.files.filterNot { it.path == file.path },
                        staleEntries = state.result.staleEntries + 1,
                    ))
            }
        }
        return current
    }
}
