package com.voyagerfiles.data.repository

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class LocalRenameTest {
    @Test fun collisionPreservesBothFiles() {
        val directory = Files.createTempDirectory("rename-test").toFile()
        try {
            val source = directory.resolve("one.txt").apply { writeText("source") }
            val occupied = directory.resolve("two.txt").apply { writeText("occupied") }
            assertTrue(runCatching { renameLocalWithoutOverwrite(source, occupied.name) }.isFailure)
            assertEquals("source", source.readText()); assertEquals("occupied", occupied.readText())
        } finally { directory.deleteRecursively() }
    }
    @Test fun caseChangePreservesContents() {
        val directory = Files.createTempDirectory("rename-case-test").toFile()
        try {
            val source = directory.resolve("report.txt").apply { writeText("payload") }
            val renamed = renameLocalWithoutOverwrite(source, "REPORT.txt")
            assertEquals("payload", renamed.readText()); assertEquals("REPORT.txt", renamed.name)
            assertEquals(1, directory.listFiles()!!.size)
        } finally { directory.deleteRecursively() }
    }
    @Test fun rejectsTraversalBeforeMutation() {
        val directory = Files.createTempDirectory("rename-path-test").toFile()
        try {
            val source = directory.resolve("one.txt").apply { writeText("safe") }
            assertTrue(runCatching { renameLocalWithoutOverwrite(source, "../escape") }.isFailure)
            assertEquals("safe", source.readText())
        } finally { directory.deleteRecursively() }
    }
}
