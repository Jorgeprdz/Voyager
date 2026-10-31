package com.voyagerfiles.ui.screens

import com.voyagerfiles.viewmodel.BrowserSession

/**
 * Display only; session ids, original titles, paths and persistence are untouched.
 * A folder really can be named "0". The complete path gives numeric titles context.
 */
internal fun mobileSessionLabel(session: BrowserSession): String {
    val title = session.title.trim()
    if (title.isNotEmpty() && !title.all(Char::isDigit)) return title
    val path = session.currentPath.trimEnd('/').ifBlank { session.rootPath.trimEnd('/') }
    if (path.isNotBlank()) {
        return if (title.isNotEmpty()) path else path.substringAfterLast('/').ifBlank { path }
    }
    return session.source.name
}
