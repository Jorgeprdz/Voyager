package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

/** Boundary checks for R3-only file operations; no actual user data is mutated. */
class FinderR3SafetyContractTest {
    @Test fun interAppTransferNeverDeletesSourceOrAcceptsFileUris() {
        assertFalse(FinderCrossAppPolicy.deletesOriginal)
        assertFalse(FinderCrossAppPolicy.accepts(listOf("file:///storage/test.txt"), true, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("https://example.org/test"), true, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("content:///no-authority"), true, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("content://authority/a"), false, false))
        assertFalse(FinderCrossAppPolicy.accepts(listOf("content://authority/a"), true, true))
        assertFalse(FinderCrossAppPolicy.accepts(List(65) { "content://authority/$it" }, true, false))
        assertTrue(FinderCrossAppPolicy.accepts(listOf("content://authority/a"), true, false))
    }

    @Test fun renamerRejectsCollisionAmongSelectedFiles() {
        val p = FinderBulkRenamePlanner.preview(
            listOf(FinderRenameCandidate("/test/a.txt", "a.txt"),
                FinderRenameCandidate("/test/b.txt", "b.txt")),
            setOf("a.txt", "b.txt"),
            FinderBulkRenameOption(FinderRenameRule.REPLACE, value = "a", replaceWith = "b")
        )
        assertFalse(p.valid)
        assertEquals(2, p.rows.size)
    }

    @Test fun renamerRejectsCaseInsensitiveUnselectedSibling() {
        val p = FinderBulkRenamePlanner.preview(
            listOf(FinderRenameCandidate("/test/alpha.txt", "alpha.txt")),
            setOf("alpha.txt", "XALPHA.TXT"),
            FinderBulkRenameOption(FinderRenameRule.PREFIX, value = "x")
        )
        assertFalse(p.valid)
        assertNotNull(p.rows.single().error)
    }

    @Test fun thousandRenamesRemainBoundedAndDeterministic() {
        val files = (0 until 1000).map {
            FinderRenameCandidate("/sandbox/f${it.toString().padStart(4, '0')}.txt",
                "f${it.toString().padStart(4, '0')}.txt")
        }
        val result = FinderBulkRenamePlanner.preview(files, files.map { it.name }.toSet(),
            FinderBulkRenameOption(FinderRenameRule.NUMBER, firstNumber = 1, digits = 4))
        assertTrue(result.valid)
        assertEquals(1000, result.changed)
        assertEquals("f0000 0001.txt", result.rows.first().newName)
        assertThrows(IllegalArgumentException::class.java) {
            FinderBulkRenamePlanner.preview(files + FinderRenameCandidate("/sandbox/extra", "extra"),
                emptySet(), FinderBulkRenameOption(FinderRenameRule.PREFIX, value = "x"))
        }
    }

    @Test fun renamerDoesNotSilentlyChangeExtensions() {
        val p = FinderBulkRenamePlanner.preview(
            listOf(FinderRenameCandidate("/test/backup.tar.gz", "backup.tar.gz")),
            emptySet(),
            FinderBulkRenameOption(FinderRenameRule.UPPER)
        )
        assertTrue(p.valid)
        assertEquals("BACKUP.TAR.gz", p.rows.single().newName)
    }
}
