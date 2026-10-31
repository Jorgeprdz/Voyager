package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderRecentPolicyTest {
    @Test fun deduplicatesNewestFirstAndClamps() {
        val events = listOf(FinderRecentOpen("/a", 1), FinderRecentOpen("/b", 2),
            FinderRecentOpen("/a", 3), FinderRecentOpen("/c", 4))
        val result = FinderRecentPolicy.bounded(events, 2) { true }
        assertEquals(listOf("/c", "/a"), result.map { it.path })
        assertEquals(3L, result.last().openedAt)
    }
    @Test fun revokedOrDeletedEntriesAreNotShown() {
        val r = FinderRecentPolicy.bounded(listOf(
            FinderRecentOpen("/gone", 4), FinderRecentOpen("/can-read", 3),
            FinderRecentOpen("content://other", 2)
        )) { it == "/can-read" }
        assertEquals(listOf("/can-read"), r.map { it.path })
    }
    @Test fun boundsAndEmptyWork() {
        assertTrue(FinderRecentPolicy.bounded(emptyList()).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            FinderRecentPolicy.bounded(emptyList(), 0)
        }
    }
}
