package com.voyagerfiles.data.archive

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExternalArchiveListingReaderTest {
    @get:Rule val files = TemporaryFolder()

    @Test
    fun listsZipWithoutExtractingFiles() = runBlocking {
        val data = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zip ->
                zip.putNextEntry(ZipEntry("notes/report.txt"))
                zip.write("hello".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val result = ExternalArchiveListingReader.read(
            "notes.zip", ByteArrayInputStream(data), files.root,
        )
        assertTrue(result.complete)
        assertEquals(listOf("notes/report.txt"), result.entries.map { it.path })
        assertTrue(files.root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun rejectsUnsafeZipPathsWithoutCreatingFiles() = runBlocking {
        val data = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zip ->
                zip.putNextEntry(ZipEntry("../escape.txt"))
                zip.write("unsafe".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()
        val result = runCatching {
            ExternalArchiveListingReader.read("unsafe.zip", ByteArrayInputStream(data), files.root)
        }
        assertTrue(result.exceptionOrNull() is UnsafeArchiveEntryException)
        assertTrue(files.root.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun tarPreviewIncludesMemberNamesButDoesNotExtract() = runBlocking {
        val data = ByteArrayOutputStream().apply {
            TarArchiveOutputStream(this).use { tar ->
                val bytes = "hello tar".toByteArray()
                val entry = TarArchiveEntry("folder/note.txt").apply { size = bytes.size.toLong() }
                tar.putArchiveEntry(entry)
                tar.write(bytes)
                tar.closeArchiveEntry()
                tar.finish()
            }
        }.toByteArray()
        val result = ExternalArchiveListingReader.read(
            "backup.tar", ByteArrayInputStream(data), files.root,
        )
        assertTrue(result.complete)
        assertEquals(listOf("folder/note.txt"), result.entries.map { it.path })
        assertTrue(files.root.listFiles().orEmpty().isEmpty())
    }
}
