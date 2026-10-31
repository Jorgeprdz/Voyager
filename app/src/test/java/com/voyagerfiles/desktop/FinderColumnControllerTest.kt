package com.voyagerfiles.desktop

import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.model.FileSource
import com.voyagerfiles.data.repository.LocalFileProvider
import com.voyagerfiles.viewmodel.BrowserSession
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FinderColumnControllerTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun session(root: String) = BrowserSession("local", "Storage", FileSource.LOCAL, root, root)
    private suspend fun settle(controller: FinderColumnController) {
        withTimeout(5000) { while (controller.columns.value.any { it.loading }) delay(5) }
    }

    @Test fun selectingOneHundredFilesKeepsPathsAndRespectsVisibleOrder() = runBlocking {
        val root = temporary.newFolder()
        repeat(100) { i -> File(root, "item-${i.toString().padStart(3, '0')}.txt").writeText("$i") }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = FinderColumnController(scope, session(root.path), LocalFileProvider()::listFiles)
            controller.start(); settle(controller)
            val items = controller.columns.value.last().files
            assertEquals(100, items.size)
            controller.selectAll()
            assertEquals(items.map { it.path }.toSet(), controller.columns.value.last().selected)
            controller.clearSelection()
            assertTrue(controller.columns.value.last().selected.isEmpty())
            val visible = items.asReversed()
            controller.selectOnly(0, visible.first(), visibleOrder = visible.map { it.path })
            controller.selectOnly(0, visible.last(), extend = true, visibleOrder = visible.map { it.path })
            assertEquals(100, controller.columns.value.last().selected.size)
            assertEquals("0", File(root, "item-000.txt").readText())
        } finally { scope.cancel() }
    }

    // Catches shared browseState or loading a different path from the selected directory.
    @Test fun localColumnsNavigateIndependentlyAndKeepAncestorSelection() = runBlocking {
        val root = temporary.newFolder()
        val child = File(root, "Documents").apply { mkdir() }
        File(child, "notes.txt").writeText("real data")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = FinderColumnController(scope, session(root.path), LocalFileProvider()::listFiles)
            controller.start(); settle(controller)
            controller.select(0, controller.columns.value[0].files.single()); settle(controller)
            assertEquals(listOf(root.path, child.path), controller.columns.value.map { it.path })
            assertEquals(setOf(child.path), controller.columns.value[0].selected)
            assertEquals("notes.txt", controller.columns.value[1].files.single().name)
            controller.select(1, controller.columns.value[1].files.single())
            assertEquals("notes.txt", controller.selectedFile?.name)
            controller.back(); settle(controller)
            assertEquals(root.path, controller.currentPath)
            controller.forward(); settle(controller)
            assertEquals(child.path, controller.currentPath)
        } finally { scope.cancel() }
    }

    // Catches accepting a late, cancellation-ignoring provider response into a replacement column.
    @Test fun staleLoadsCannotReplaceNewerColumns() = runBlocking {
        val slow = CompletableDeferred<Result<List<FileItem>>>()
        val started = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = FinderColumnController(scope, session("/root"), { path ->
                when (path) {
                    "/root" -> Result.success(listOf(FileItem("slow", "/root/slow", true), FileItem("fast", "/root/fast", true)))
                    "/root/slow" -> withContext(NonCancellable) { started.complete(Unit); slow.await() }
                    else -> Result.success(listOf(FileItem("correct", "/root/fast/correct", false)))
                }
            })
            controller.start(); settle(controller)
            controller.select(0, controller.columns.value[0].files[1]) // sorted: fast, slow
            withTimeout(5000) { started.await() }
            controller.select(0, controller.columns.value[0].files[0])
            settle(controller)
            slow.complete(Result.success(listOf(FileItem("obsolete", "/root/slow/obsolete", false))))
            yield()
            assertEquals("/root/fast", controller.columns.value.last().path)
            assertEquals("correct", controller.columns.value.last().files.single().name)
        } finally { scope.cancel() }
    }

    @Test fun errorsArePerColumnAndCanBeRetried() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var denied = true
        try {
            val controller = FinderColumnController(scope, session("/root"), {
                if (denied) Result.failure(SecurityException("Permission revoked")) else Result.success(emptyList())
            })
            controller.start(); settle(controller)
            assertEquals("Permission revoked", controller.columns.value.single().error)
            denied = false; controller.refresh(); settle(controller)
            assertNull(controller.columns.value.single().error)
        } finally { scope.cancel() }
    }

    @Test fun restoreUsesProviderPathsAndPreservesSelectionAndScroll() = runBlocking {
        val root = temporary.newFolder()
        val file = File(root, "file.txt").apply { writeText("safe") }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = FinderColumnController(scope, session(root.path), LocalFileProvider()::listFiles)
            controller.start(); settle(controller)
            controller.select(0, controller.columns.value.single().files.single())
            controller.rememberScroll(0, 3, 12)
            controller.reconcile(root.path, setOf(file.path))
            assertEquals(setOf(file.path), controller.columns.value.single().selected)
            assertEquals(3, controller.columns.value.single().scrollIndex)
            assertEquals(12, controller.columns.value.single().scrollOffset)
            assertEquals("safe", file.readText())
        } finally { scope.cancel() }
    }

    @Test fun iconSelectionDoesNotOpenFolderUntilActivated() = runBlocking {
        val root = temporary.newFolder()
        val child = File(root, "Documents").apply { mkdir() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = FinderColumnController(scope, session(root.path), LocalFileProvider()::listFiles)
            controller.start(); settle(controller)
            val folder = controller.columns.value.single().files.single()
            controller.selectOnly(0, folder)
            assertEquals(root.path, controller.currentPath)
            assertEquals(child.path, controller.selectedFile?.path)
            assertEquals(1, controller.columns.value.size)
            controller.select(0, folder); settle(controller)
            assertEquals(child.path, controller.currentPath)
        } finally { scope.cancel() }
    }


    @Test fun additiveAndRangeSelectionOnlyAffectCurrentDirectory() = runBlocking {
        val root = temporary.newFolder()
        val docs = File(root, "Documents").apply { mkdir() }
        listOf("a.txt", "b.txt", "c.txt", "d.txt").forEach { File(docs, it).writeText(it) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = FinderColumnController(scope, session(root.path), LocalFileProvider()::listFiles)
            controller.start(); settle(controller)
            controller.select(0, controller.columns.value.first().files.single()); settle(controller)
            val ordered = controller.columns.value.last().files
            controller.selectOnly(1, ordered[0])
            controller.selectOnly(1, ordered[2], additive = true)
            assertEquals(setOf(ordered[0].path, ordered[2].path), controller.columns.value.last().selected)
            controller.selectOnly(1, ordered[3], extend = true)
            assertEquals(setOf(ordered[2].path, ordered[3].path), controller.columns.value.last().selected)
            controller.selectAll()
            assertEquals(4, controller.columns.value.last().selected.size)
            assertEquals(setOf(docs.path), controller.columns.value.first().selected)
            assertEquals("a.txt", File(docs, "a.txt").readText())
        } finally { scope.cancel() }
    }

    @Test fun windowModeUsesActualWindowAndHeight() {
        assertEquals(DesktopMode.COMPACT, DesktopModeController.mode(500f, 700f))
        assertEquals(DesktopMode.ADAPTED, DesktopModeController.mode(700f, 700f))
        assertEquals(DesktopMode.FULL, DesktopModeController.mode(1200f, 700f))
        assertEquals(DesktopMode.COMPACT, DesktopModeController.mode(1200f, 250f))
    }
}
