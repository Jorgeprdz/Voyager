package com.voyagerfiles.desktop

import org.junit.Assert.*
import org.junit.Test

class FinderBulkRenamePlannerTest {
    private val sample = listOf(FinderRenameCandidate("/x/b.txt", "b.txt"), FinderRenameCandidate("/x/a.txt", "a.txt"))
    @Test fun preservesExtensionAndDeterministicOrder() {
        val p = FinderBulkRenamePlanner.preview(sample, setOf("a.txt","b.txt"),
            FinderBulkRenameOption(FinderRenameRule.NUMBER, firstNumber=8, digits=3))
        assertTrue(p.valid)
        assertEquals(listOf("a 008.txt","b 009.txt"), p.rows.map { it.newName })
    }
    @Test fun rejectsSiblingAndRepeatedTargets() {
        val a=FinderBulkRenamePlanner.preview(sample, setOf("a.txt","b.txt","pre-a.txt"),
            FinderBulkRenameOption(FinderRenameRule.PREFIX,value="pre-"))
        assertFalse(a.valid)
        val b=FinderBulkRenamePlanner.preview(sample, emptySet(),
            FinderBulkRenameOption(FinderRenameRule.REPLACE,value=".txt",replaceWith=".dat",changeExtension=true))
        assertTrue(b.valid)
    }
    @Test fun catchesInvalidAndExtremeNames() {
        val a=FinderBulkRenamePlanner.preview(sample, emptySet(),
            FinderBulkRenameOption(FinderRenameRule.PREFIX,value="../"))
        assertFalse(a.valid)
        val b=FinderBulkRenamePlanner.preview(sample, emptySet(),
            FinderBulkRenameOption(FinderRenameRule.PREFIX,value="🚀".repeat(100)))
        assertFalse(b.valid)
    }
    @Test fun directoryDotsAreNotExtensions() {
        val result = FinderBulkRenamePlanner.preview(listOf(FinderRenameCandidate("/x/folder.old", "folder.old", true)), emptySet(),
            FinderBulkRenameOption(FinderRenameRule.SUFFIX, value = "-new"))
        assertEquals("folder.old-new", result.rows.single().newName)
    }
    @Test(expected = IllegalArgumentException::class) fun numberingDoesNotOverflow() {
        FinderBulkRenamePlanner.preview(sample, emptySet(), FinderBulkRenameOption(FinderRenameRule.NUMBER, firstNumber = Int.MAX_VALUE))
    }
}
