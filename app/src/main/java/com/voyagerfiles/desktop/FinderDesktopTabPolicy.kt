package com.voyagerfiles.desktop

import com.voyagerfiles.data.model.FileSource
import com.voyagerfiles.viewmodel.BrowserSession

/** The Desktop tab strip only lists local browser sessions; remote sessions remain in Voyager. */
internal object FinderDesktopTabPolicy {
    const val MAX_LOCAL_TABS = 12
    fun visible(sessions: List<BrowserSession>): List<BrowserSession> =
        sessions.filter { it.source == FileSource.LOCAL }

    fun canAdd(sessions: List<BrowserSession>): Boolean =
        visible(sessions).size < MAX_LOCAL_TABS

    fun nextId(sessions: List<BrowserSession>, selectedId: String?): String? {
        val tabs = visible(sessions)
        if (tabs.isEmpty()) return null
        val i = tabs.indexOfFirst { it.id == selectedId }
        return tabs[(i + 1).mod(tabs.size)].id
    }
}
