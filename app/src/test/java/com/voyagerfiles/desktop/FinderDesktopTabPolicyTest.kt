package com.voyagerfiles.desktop

import com.voyagerfiles.data.model.FileSource
import com.voyagerfiles.viewmodel.BrowserSession
import org.junit.Assert.*
import org.junit.Test

class FinderDesktopTabPolicyTest {
    private fun tab(id: String, source: FileSource = FileSource.LOCAL) = BrowserSession(
        id=id, title=id, source=source, rootPath="/root", currentPath="/root"
    )
    @Test fun excludesRemoteSessionsAndWraps() {
        val all=listOf(tab("a"),tab("sftp",FileSource.SFTP),tab("b"))
        assertEquals(listOf("a","b"),FinderDesktopTabPolicy.visible(all).map{it.id})
        assertEquals("b",FinderDesktopTabPolicy.nextId(all,"a"))
        assertEquals("a",FinderDesktopTabPolicy.nextId(all,"b"))
    }
    @Test fun limitsConcurrentLocalTabs() {
        assertTrue(FinderDesktopTabPolicy.canAdd(List(11){tab("id$it")}))
        assertFalse(FinderDesktopTabPolicy.canAdd(List(12){tab("id$it")}))
    }
}
