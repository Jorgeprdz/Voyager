package com.voyagerfiles.desktop

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.voyagerfiles.data.model.FileSource
import com.voyagerfiles.data.repository.LocalFileProvider
import com.voyagerfiles.viewmodel.BrowserSession
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

class FinderDesktopScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var root: File
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Before fun setup() {
        root = File(ApplicationProvider.getApplicationContext<Application>().cacheDir, "finderdex-ui-${System.nanoTime()}")
        File(root, "Documents").mkdirs()
        File(root, "Documents/notes.txt").writeText("FinderDex test")
    }
    @After fun cleanup() { scope.cancel(); root.deleteRecursively() }

    @Test fun startupIsIconViewAndFolderSelectionIsSeparateFromOpening() {
        val controller = FinderColumnController(scope,
            BrowserSession("icons", "Test", FileSource.LOCAL, root.path, root.path), LocalFileProvider()::listFiles)
        var opened: String? = null
        compose.setContent { FinderDesktopScreen(controller, DesktopMode.FULL, onOpen = { opened = it.path }) }
        compose.runOnIdle { controller.start() }
        compose.waitUntil(10_000) { controller.columns.value.firstOrNull()?.loading == false }
        compose.onNodeWithTag("finder-icons").assertExists()
        compose.onNodeWithTag("finder-columns").assertDoesNotExist()
        compose.onNodeWithText("Documents").performClick()
        compose.runOnIdle {
            assertEquals(root.path, controller.currentPath)
            assertEquals(File(root, "Documents").path, controller.selectedFile?.path)
        }
        compose.onNodeWithText("Documents").performTouchInput { doubleClick() }
        compose.waitUntil(10_000) { controller.columns.value.size == 2 && !controller.columns.value.last().loading }
        compose.onNodeWithText("notes.txt").performClick()
        compose.runOnIdle { assertNull(opened) }
        compose.onNodeWithText("notes.txt").performTouchInput { doubleClick() }
        compose.runOnIdle { assertEquals(File(root, "Documents/notes.txt").path, opened) }
    }


    @Test fun compactSelectorUsesRealCallbacksForEveryMode() {
        val controller = FinderColumnController(scope,
            BrowserSession("compact-selector","Test",FileSource.LOCAL,root.path,root.path),
            LocalFileProvider()::listFiles)
        compose.setContent {
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.width(560.dp).height(600.dp)) {
                FinderDesktopScreen(controller,DesktopMode.FULL)
            }
        }
        compose.runOnIdle { controller.start() }
        compose.waitUntil(10_000) { controller.columns.value.lastOrNull()?.loading == false }
        fun choose(title:String) {
            compose.onNodeWithContentDescription("Elegir vista").performClick()
            compose.onNodeWithText(title).performClick()
        }
        choose("Vista de lista")
        compose.onNodeWithTag("finder-list").assertExists()
        choose("Vista de galería")
        compose.onNodeWithTag("finder-gallery").assertExists()
        choose("Column view")
        compose.onNodeWithTag("finder-columns").assertExists()
        choose("Icon view")
        compose.onNodeWithTag("finder-icons").assertExists()
        compose.runOnIdle { assertEquals(root.path,controller.currentPath) }
    }

    @Test fun listAndGalleryHaveRealNavigationAndPreserveSelectedFile() {
        val controller = FinderColumnController(scope,
            BrowserSession("all-views", "Test", FileSource.LOCAL, root.path, root.path),
            LocalFileProvider()::listFiles)
        compose.setContent { FinderDesktopScreen(controller, DesktopMode.FULL) }
        compose.runOnIdle { controller.start() }
        compose.waitUntil(10_000) { controller.columns.value.firstOrNull()?.loading == false }
        compose.onNodeWithContentDescription("Vista de lista").performClick()
        compose.onNodeWithTag("finder-list").assertExists()
        compose.onNodeWithText("Documents").performTouchInput { doubleClick() }
        compose.waitUntil(10_000) { controller.columns.value.lastOrNull()?.path == File(root, "Documents").path &&
            controller.columns.value.lastOrNull()?.loading == false }
        compose.onNodeWithText("notes.txt").performClick()
        compose.onNodeWithContentDescription("Vista de galería").performClick()
        compose.onNodeWithTag("finder-gallery").assertExists()
        compose.runOnIdle {
            assertEquals(File(root, "Documents/notes.txt").path, controller.selectedFile?.path)
        }
        compose.onNodeWithContentDescription(ApplicationProvider.getApplicationContext<Application>()
            .getString(com.voyagerfiles.R.string.finderdex_view_columns)).performClick()
        compose.onNodeWithTag("finder-columns").assertExists()
        compose.runOnIdle { assertEquals(File(root, "Documents").path, controller.currentPath) }
    }

    @Test fun columnsNavigateSelectOpenAndKeepStateAcrossLayoutChange() {
        val controller = FinderColumnController(scope,
            BrowserSession("test", "Test", FileSource.LOCAL, root.path, root.path), LocalFileProvider()::listFiles)
        var mode by mutableStateOf(DesktopMode.FULL)
        var opened: String? = null
        compose.setContent { FinderDesktopScreen(controller, mode, onOpen = { opened = it.path }) }
        compose.runOnIdle { controller.start() }
        compose.waitUntil(10_000) { controller.columns.value.firstOrNull()?.loading == false }
        compose.onNodeWithContentDescription(ApplicationProvider.getApplicationContext<Application>().getString(com.voyagerfiles.R.string.finderdex_view_columns)).performClick()
        compose.onNodeWithText("Documents").performClick()
        compose.waitUntil(10_000) { controller.columns.value.size == 2 && !controller.columns.value.last().loading }
        compose.onNodeWithText("notes.txt").performClick()
        compose.runOnIdle { mode = DesktopMode.ADAPTED }
        compose.onNodeWithText("notes.txt").assertIsSelected()
        compose.runOnIdle { mode = DesktopMode.FULL }
        compose.onNodeWithContentDescription("Mostrar vista previa").performClick()
        compose.onNodeWithText("Abrir").performClick()
        compose.runOnIdle { assertEquals(File(root, "Documents/notes.txt").path, opened) }
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.waitUntil(10_000) { controller.columns.value.size == 1 }
        compose.onNodeWithContentDescription("Adelante").performClick()
        compose.waitUntil(10_000) { controller.currentPath == File(root, "Documents").path }
    }
}
