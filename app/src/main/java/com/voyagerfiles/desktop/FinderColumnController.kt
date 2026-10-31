package com.voyagerfiles.desktop

import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.viewmodel.BrowserSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** A directory column owns its load identity, selection and scroll independently. */
data class FinderColumn(
    val id: Long,
    val path: String,
    val files: List<FileItem> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val selected: Set<String> = emptySet(),
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
)

class FinderColumnController(
    private val scope: CoroutineScope,
    val session: BrowserSession,
    private val listDirectory: suspend (String) -> Result<List<FileItem>>,
    private val parentOf: (String) -> String? = { java.io.File(it).parent },
    private val onLocation: (String, Set<String>) -> Unit = { _, _ -> },
) {
    private val _columns = MutableStateFlow<List<FinderColumn>>(emptyList())
    val columns = _columns.asStateFlow()
    private var nextId = 0L
    private var selectionAnchor: String? = null
    private var keyboardCursor: String? = null
    private val jobs = mutableMapOf<Long, Job>()
    private val past = ArrayDeque<List<FinderColumn>>()
    private val future = ArrayDeque<List<FinderColumn>>()
    val currentPath get() = _columns.value.lastOrNull()?.path ?: session.currentPath
    val canBack get() = past.isNotEmpty()
    val canForward get() = future.isNotEmpty()
    val selectedFile: FileItem? get() = _columns.value.asReversed().firstNotNullOfOrNull { c -> c.files.firstOrNull { it.path in c.selected } }

    fun start() { if (_columns.value.isEmpty()) rebuild(session.currentPath, emptySet()) }

    private fun chain(path: String): List<String>? {
        val result = mutableListOf<String>()
        var current: String? = path
        while (current != null && current !in result && result.size < 256) {
            result.add(current)
            if (current == session.rootPath) return result.asReversed()
            current = parentOf(current)
        }
        return null
    }

    private fun rebuild(path: String, selected: Set<String>) {
        selectionAnchor = null
        val paths = chain(path) ?: return
        cancelLoads()
        _columns.value = paths.mapIndexed { i, p ->
            FinderColumn(++nextId, p, selected = if (i == paths.lastIndex) selected else setOf(paths[i + 1]))
        }
        _columns.value.forEach(::load)
    }

    fun reconcile(path: String, selected: Set<String>) {
        if (path != currentPath) {
            past.clear(); future.clear(); rebuild(path, selected)
        } else {
            _columns.update { columns -> columns.mapIndexed { i, c ->
                if (i == columns.lastIndex) c.copy(selected = selected) else c
            } }
        }
    }

    /** Icon view selects a folder on one click; activation opens it separately. */
    fun selectOnly(index: Int, file: FileItem, additive: Boolean = false, extend: Boolean = false) {
        val columns = _columns.value
        val column = columns.getOrNull(index) ?: return
        if (index != columns.lastIndex || column.files.none { it.path == file.path }) return
        val selection = when {
            extend -> {
                val anchor = column.files.indexOfFirst { it.path == selectionAnchor }
                val target = column.files.indexOfFirst { it.path == file.path }
                if (anchor < 0) setOf(file.path)
                else column.files.subList(minOf(anchor, target), maxOf(anchor, target) + 1)
                    .mapTo(mutableSetOf()) { it.path }
            }
            additive -> column.selected.toMutableSet().apply {
                if (!add(file.path)) remove(file.path)
            }
            else -> setOf(file.path)
        }
        if (!extend) selectionAnchor = file.path
        keyboardCursor = file.path
        _columns.value = columns.mapIndexed { i, c -> if (i == index) c.copy(selected = selection) else c }
        onLocation(currentPath, selection)
    }

    /** One authoritative selection for mouse, keyboard, marquee and file operations. */
    fun selectPaths(paths: Set<String>, extendExisting: Boolean = false) {
        val columns = _columns.value
        val last = columns.lastOrNull() ?: return
        if (last.loading || last.error != null) return
        val valid = last.files.mapTo(mutableSetOf()) { it.path }
        val selection = (if (extendExisting) last.selected + paths else paths).intersect(valid)
        _columns.value = columns.mapIndexed { index, column ->
            if (index == columns.lastIndex) column.copy(selected = selection) else column
        }
        if (selection.isEmpty()) { selectionAnchor = null; keyboardCursor = null }
        else if (selectionAnchor !in selection) selectionAnchor = selection.first()
        onLocation(currentPath, selection)
    }

    fun clearSelection() = selectPaths(emptySet())

    /** The visible file order is also keyboard navigation order. */
    fun moveSelectionBy(delta: Int, extend: Boolean = false) {
        val last = _columns.value.lastOrNull() ?: return
        if (last.files.isEmpty() || last.loading) return
        val anchor = last.files.indexOfFirst { it.path == selectionAnchor }
        val current = last.files.indexOfFirst { it.path == keyboardCursor }
            .takeIf { it >= 0 } ?: last.files.indexOfFirst { it.path in last.selected }
        val start = if (current >= 0) current else if (delta >= 0) -1 else last.files.size
        val target = (start + delta).coerceIn(0, last.files.lastIndex)
        keyboardCursor = last.files[target].path
        if (extend) {
            val from = if (anchor >= 0) anchor else (if (current >= 0) current else target)
            selectionAnchor = last.files[from].path
            selectPaths(last.files.subList(minOf(from, target), maxOf(from, target) + 1)
                .mapTo(mutableSetOf()) { it.path })
        } else {
            selectOnly(_columns.value.lastIndex, last.files[target])
        }
    }

    /** Selects the loaded directory only, never ancestors or hidden items. */
    fun selectAll() {
        val last = _columns.value.lastOrNull() ?: return
        if (last.loading || last.error != null) return
        val selection = last.files.mapTo(mutableSetOf()) { it.path }
        selectionAnchor = last.files.firstOrNull()?.path
        _columns.update { columns -> columns.mapIndexed { i, c ->
            if (i == columns.lastIndex) c.copy(selected = selection) else c
        } }
        onLocation(currentPath, selection)
    }

    fun select(index: Int, file: FileItem) {
        selectionAnchor = file.path
        val original = _columns.value
        val column = original.getOrNull(index) ?: return
        if (column.files.none { it.path == file.path }) return
        val nextPath = if (file.isDirectory) file.path else column.path
        if (nextPath != currentPath) {
            past.addLast(original); if (past.size > 100) past.removeFirst(); future.clear()
        }
        val kept = original.take(index + 1).mapIndexed { i, c ->
            if (i == index) c.copy(selected = setOf(file.path)) else c
        }
        val next = if (file.isDirectory) kept + FinderColumn(++nextId, file.path) else kept
        val retained = next.mapTo(mutableSetOf()) { it.id }
        jobs.keys.toList().filter { it !in retained }.forEach { jobs.remove(it)?.cancel() }
        _columns.value = next
        if (file.isDirectory) load(next.last())
        onLocation(nextPath, if (file.isDirectory) emptySet() else setOf(file.path))
    }

    fun navigateColumn(index: Int) {
        if (index !in _columns.value.indices || index == _columns.value.lastIndex) return
        past.addLast(_columns.value); future.clear()
        restore(_columns.value.take(index + 1).mapIndexed { i, c -> if (i == index) c.copy(selected = emptySet()) else c })
    }

    fun back() {
        if (past.isEmpty()) return
        future.addLast(_columns.value); restore(past.removeLast())
    }
    fun forward() {
        if (future.isEmpty()) return
        past.addLast(_columns.value); restore(future.removeLast())
    }
    private fun restore(snapshot: List<FinderColumn>) {
        cancelLoads()
        _columns.value = snapshot.map { it.copy(id = ++nextId, loading = true, error = null) }
        _columns.value.forEach(::load)
        onLocation(currentPath, _columns.value.lastOrNull()?.selected.orEmpty())
    }
    fun refresh() {
        restore(_columns.value)
    }
    fun rememberScroll(index: Int, first: Int, offset: Int) {
        _columns.update { columns -> columns.mapIndexed { i, c ->
            if (i == index) c.copy(scrollIndex = first, scrollOffset = offset) else c
        } }
    }
    private fun load(column: FinderColumn) {
        jobs[column.id] = scope.launch {
            val result = try {
                withContext(Dispatchers.IO) { listDirectory(column.path) }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { Result.failure(error) }
            ensureActive()
            _columns.update { columns -> columns.map { c ->
                if (c.id != column.id) c else c.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.localizedMessage,
                    files = result.getOrDefault(emptyList()).filterNot { it.isHidden }
                        .sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                )
            } }
        }
    }
    private fun cancelLoads() { jobs.values.forEach { it.cancel() }; jobs.clear() }
    fun close() { cancelLoads() }
}
