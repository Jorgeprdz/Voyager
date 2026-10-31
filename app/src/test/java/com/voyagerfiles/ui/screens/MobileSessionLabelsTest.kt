package com.voyagerfiles.ui.screens

import com.voyagerfiles.data.model.FileSource
import com.voyagerfiles.viewmodel.BrowserSession
import org.junit.Assert.assertEquals
import org.junit.Test

class MobileSessionLabelsTest {
    private fun session(title: String, path: String) =
        BrowserSession("id", title, FileSource.LOCAL, "/storage/emulated/0", path)

    @Test fun numericTitleUsesFullPathNotBareZero() {
        assertEquals("/storage/emulated/0", mobileSessionLabel(session("0", "/storage/emulated/0")))
    }
    @Test fun customTitleRemainsUnchanged() {
        assertEquals("Fotos", mobileSessionLabel(session("Fotos", "/storage/emulated/0/DCIM")))
    }
    @Test fun emptyTitleUsesFolderName() {
        assertEquals("DCIM", mobileSessionLabel(session("", "/storage/emulated/0/DCIM")))
    }
}
