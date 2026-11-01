package com.voyagerfiles.data.archive

import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

data class ExternalArchiveEntry(val path: String, val isDirectory: Boolean, val size: Long?)
data class ExternalArchiveListing(val entries: List<ExternalArchiveEntry>, val complete: Boolean)

/** A bounded, read-only preview. Extraction remains owned by ArchiveService. */
object ExternalArchiveListingReader {
    private const val MAX_ENTRIES = 250
    private const val MAX_ZIP_SOURCE = 128L * 1024 * 1024
    private const val MAX_TAR_SCAN = 256L * 1024 * 1024

    suspend fun read(name: String, input: InputStream, cacheDir: File): ExternalArchiveListing =
        withContext(Dispatchers.IO) {
            val format = ArchiveFormat.detect(name)
                ?: throw UnsupportedArchiveException(null, "Unsupported archive format")
            if (!format.canExtract) {
                throw UnsupportedArchiveException(format, "RAR is recognized but not supported in this build")
            }
            input.use { stream ->
                when (format) {
                    ArchiveFormat.ZIP -> inspectZip(stream, cacheDir)
                    ArchiveFormat.TAR -> inspectTar(stream)
                    ArchiveFormat.TAR_GZIP -> GzipCompressorInputStream.builder()
                        .setInputStream(stream).setDecompressConcatenated(true).get().use { inspectTar(it) }
                    ArchiveFormat.TAR_BZIP2 -> BZip2CompressorInputStream(stream, true).use { inspectTar(it) }
                    ArchiveFormat.GZIP, ArchiveFormat.BZIP2 -> {
                        ExternalArchiveListing(listOf(ExternalArchiveEntry(format.stem(name), false, null)), true)
                    }
                    ArchiveFormat.RAR_UNSUPPORTED -> error("RAR was already rejected")
                }
            }
        }

    private suspend fun inspectZip(input: InputStream, cacheDir: File): ExternalArchiveListing {
        val spooled = File.createTempFile("finderdex-external-", ".zip", cacheDir)
        try {
            spooled.outputStream().buffered().use { output ->
                val buffer = ByteArray(64 * 1024)
                var copied = 0L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied += read
                    if (copied > MAX_ZIP_SOURCE) throw IllegalArgumentException("Preview limit of 128 MB exceeded")
                    output.write(buffer, 0, read)
                }
            }
            ZipFile(spooled).use { zip ->
                val entries = mutableListOf<ExternalArchiveEntry>()
                val cursor = zip.entries()
                while (cursor.hasMoreElements() && entries.size < MAX_ENTRIES) {
                    currentCoroutineContext().ensureActive()
                    val entry = cursor.nextElement()
                    ArchiveEntryPath.parse(entry.name).getOrThrow()
                    entries += ExternalArchiveEntry(entry.name, entry.isDirectory, entry.size.takeIf { it >= 0 })
                }
                return ExternalArchiveListing(entries, !cursor.hasMoreElements())
            }
        } finally {
            spooled.delete()
        }
    }

    private suspend fun inspectTar(source: InputStream): ExternalArchiveListing {
        val input = ByteLimitInputStream(source, MAX_TAR_SCAN)
        TarArchiveInputStream(input).use { tar ->
            val entries = mutableListOf<ExternalArchiveEntry>()
            var complete = false
            while (entries.size < MAX_ENTRIES) {
                currentCoroutineContext().ensureActive()
                val entry = tar.nextEntry
                if (entry == null) {
                    complete = true
                    break
                }
                ArchiveEntryPath.parse(entry.name).getOrThrow()
                if (entry.isSymbolicLink || entry.isLink || (!entry.isDirectory && !entry.isFile)) {
                    throw UnsafeArchiveEntryException(entry.name, "unsafe TAR member")
                }
                entries += ExternalArchiveEntry(entry.name, entry.isDirectory, entry.size.takeIf { it >= 0 })
            }
            return ExternalArchiveListing(entries, complete)
        }
    }

    private class ByteLimitInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
        private var consumed = 0L
        override fun read(): Int {
            if (consumed >= limit) throw IllegalArgumentException("Archive preview exceeds safe scan limit")
            return super.read().also { if (it >= 0) consumed++ }
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (consumed >= limit) throw IllegalArgumentException("Archive preview exceeds safe scan limit")
            val amount = super.read(buffer, offset, minOf(length.toLong(), limit - consumed).toInt())
            if (amount > 0) consumed += amount
            return amount
        }
        override fun skip(n: Long): Long {
            if (consumed >= limit) throw IllegalArgumentException("Archive preview exceeds safe scan limit")
            return super.skip(minOf(n, limit - consumed)).also { consumed += it }
        }
    }
}
