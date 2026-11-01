package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderCrossAppPolicyTest {
    @Test fun contentUrisCanBeCopied() {
        assertTrue(FinderCrossAppPolicy.accepts(listOf("content://media/external/images/1"), true, false))
        assertTrue(FinderCrossAppPolicy.accepts(listOf("content://a/1", "content://b/2"), true, false))
        assertFalse(FinderCrossAppPolicy.deletesOriginal)
    }

    @Test fun rejectsUnsafeOrMissingUris() {
        assertFalse(FinderCrossAppPolicy.accepts(emptyList(), true, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("file:///sdcard/private.txt"), true, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("https://example.com/x"), true, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("content:///no-authority"), true, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("content://media/1", "file:///tmp/2"), true, false))
    }

    @Test fun rejectsBusyOrReadOnlyAndExcessiveDrops() {
        val one = listOf("content://media/1")
        assertFalse(FinderCrossAppPolicy.accepts(one, false, false))
        assertFalse(FinderCrossAppPolicy.accepts(one, true, true))
        assertFalse(FinderCrossAppPolicy.accepts(List(65) { "content://media/$it" }, true, false))
        assertTrue(FinderCrossAppPolicy.accepts(List(64) { "content://media/$it" }, true, false))
    }
}
