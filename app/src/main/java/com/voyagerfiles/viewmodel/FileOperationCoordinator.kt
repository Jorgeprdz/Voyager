package com.voyagerfiles.viewmodel

import com.voyagerfiles.data.model.FileItem
import com.voyagerfiles.data.repository.TransferCancellation
import com.voyagerfiles.data.repository.FileProvider
import com.voyagerfiles.data.repository.StreamTransferProgress
import java.util.UUID
import java.security.MessageDigest
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DestinationConflictException(identifierOrName: String) :
    IllegalStateException("An item named ${identifierOrName.substringAfterLast('/')} already exists in this folder")

object FileOperationCoordinator {
    private val rejectConflict: ConflictResolver = { throw DestinationConflictException(it.destination.name) }

    suspend fun uploadFile(
        source: UploadSource,
        destinationProvider: FileProvider,
        destinationDirectoryPath: String,
        resolveConflict: ConflictResolver = rejectConflict,
        onProgress: (StreamTransferProgress) -> Unit = {},
    ): Result<TransferDisposition> = withContext(Dispatchers.IO) {
        runCatching {
            transfer(
                destinationProvider, destinationDirectoryPath,
                ConflictSource(source.name, source.size, source.lastModified), false, resolveConflict,
                validateTarget = { target ->
                    require(source.identifier == null || !destinationProvider.isSamePath(source.identifier, target.path)) {
                        "A file cannot replace itself"
                    }
                },
            ) { target ->
                source.openInputStream().use { input ->
                    destinationProvider.writeStream(target, input, source.name, source.size, onProgress).getOrThrow()
                }
            }
        }
    }

    suspend fun copyPath(
        sourceProvider: FileProvider,
        destinationProvider: FileProvider,
        sourcePath: String,
        destinationDirectoryPath: String,
        resolveConflict: ConflictResolver = rejectConflict,
        onProgress: (StreamTransferProgress) -> Unit = {},
        destinationName: String? = null,
    ): Result<TransferDisposition> = withContext(Dispatchers.IO) {
        runCatching {
            copyPathInternal(sourceProvider, destinationProvider, sourcePath,
                destinationDirectoryPath, resolveConflict, onProgress, destinationName)
        }
    }

    suspend fun movePath(
        sourceProvider: FileProvider,
        destinationProvider: FileProvider,
        sourcePath: String,
        destinationDirectoryPath: String,
        resolveConflict: ConflictResolver = rejectConflict,
        onProgress: (StreamTransferProgress) -> Unit = {},
    ): Result<TransferDisposition> = withContext(Dispatchers.IO) {
        runCatching {
            val disposition = copyPathInternal(sourceProvider, destinationProvider, sourcePath, destinationDirectoryPath, resolveConflict, onProgress)
            if (disposition == TransferDisposition.COMPLETED) {
                // Never delete the source on a remote provider's success claim alone.
                // Reopen both committed trees and verify bytes, not only nominal sizes.
                val source = sourceProvider.getFileInfo(sourcePath).getOrThrow()
                val destination = destinationProvider.listFiles(destinationDirectoryPath).getOrThrow()
                    .singleOrNull { it.name == source.name && it.isDirectory == source.isDirectory }
                    ?: error("Copied destination is missing or ambiguous; source retained")
                verifyCommittedCopy(sourceProvider, destinationProvider, source, destination)
                // Cancellation or verification failure keeps both copies for manual resolution.
                TransferCancellation.check()
                sourceProvider.delete(sourcePath).getOrThrow()
            }
            disposition
        }
    }

    /**
     * Validate a committed cross-provider (or local) copy before a destructive move.
     * Inability to read or verify the destination fails closed and retains the source.
     */
    private suspend fun verifyCommittedCopy(
        sourceProvider: FileProvider,
        destinationProvider: FileProvider,
        source: FileItem,
        destination: FileItem,
        depth: Int = 0,
        visited: MutableSet<String> = mutableSetOf(),
    ) {
        TransferCancellation.check()
        check(depth <= 128) { "Verification exceeded safe folder depth; source retained" }
        check(source.isDirectory == destination.isDirectory && source.name == destination.name) {
            "Destination differs from source; source retained"
        }
        check(visited.add(source.path)) { "Repeated folder or cycle during verification; source retained" }
        if (source.isDirectory) {
            val children = sourceProvider.listFiles(source.path).getOrThrow()
            val copied = destinationProvider.listFiles(destination.path).getOrThrow()
            check(children.size == copied.size) { "Copied folder does not contain the same entries; source retained" }
            check(children.map { it.name }.toSet().size == children.size &&
                copied.map { it.name }.toSet().size == copied.size) {
                "Ambiguous names in copied folder; source retained"
            }
            val byName = copied.associateBy { it.name }
            for (child in children) {
                TransferCancellation.check()
                val target = byName[child.name] ?: error("Missing copied item: ${child.name}; source retained")
                verifyCommittedCopy(sourceProvider, destinationProvider, child, target, depth + 1, visited)
            }
        } else {
            if (source.size > 0L && destination.size > 0L) {
                check(source.size == destination.size) { "Copied file size mismatch; source retained" }
            }
            val fromHash = sourceProvider.getInputStream(source.path).getOrThrow().use { sha256(it) }
            val toHash = destinationProvider.getInputStream(destination.path).getOrThrow().use(::sha256)
            check(MessageDigest.isEqual(fromHash, toHash)) {
                "Copied file content does not match source; source retained"
            }
        }
    }

    private fun sha256(input: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            TransferCancellation.check()
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) digest.update(buffer, 0, count)
        }
        return digest.digest()
    }

    private suspend fun copyPathInternal(
        sourceProvider: FileProvider,
        destinationProvider: FileProvider,
        sourcePath: String,
        destinationDirectoryPath: String,
        resolveConflict: ConflictResolver,
        onProgress: (StreamTransferProgress) -> Unit,
        destinationName: String? = null,
    ): TransferDisposition {
        TransferCancellation.check()
        val item = sourceProvider.getFileInfo(sourcePath).getOrThrow()
        require(destinationName == null || (destinationName.isNotBlank() &&
            destinationName != "." && destinationName != ".." &&
            '/' !in destinationName && '\\' !in destinationName)) { "Invalid duplicate name" }
        if (item.isDirectory) {
            require(!sourceProvider.isDescendantPath(sourcePath, destinationProvider, destinationDirectoryPath)) {
                "A folder cannot be copied or moved into itself"
            }
        }
        return transfer(
            destinationProvider, destinationDirectoryPath,
            ConflictSource(destinationName ?: item.name, item.size.takeIf { it >= 0 }, item.lastModified), item.isDirectory, resolveConflict,
            validateTarget = { target ->
                require(!sourceProvider.isSamePath(sourcePath, destinationProvider, target.path)) { "An item cannot replace itself" }
                require(!target.isDirectory || !destinationProvider.isDescendantPath(target.path, sourceProvider, sourcePath)) {
                    "A folder containing the source cannot be replaced"
                }
            },
        ) { target ->
            copyContents(sourceProvider, destinationProvider, item, target, onProgress)
        }
    }

    private suspend fun copyContents(
        sourceProvider: FileProvider,
        destinationProvider: FileProvider,
        item: FileItem,
        target: String,
        onProgress: (StreamTransferProgress) -> Unit,
    ) {
        if (item.isDirectory) {
            sourceProvider.listFiles(item.path).getOrThrow().forEach { child ->
                copyPathInternal(sourceProvider, destinationProvider, child.path, target, rejectConflict, onProgress)
            }
        } else if (sourceProvider === destinationProvider) {
            sourceProvider.copyFileTo(item.path, target, item.size.takeIf { it >= 0 }, onProgress).getOrThrow()
        } else {
            sourceProvider.getInputStream(item.path).getOrThrow().use { input ->
                destinationProvider.writeStream(target, input, item.path, item.size.takeIf { it >= 0 }, onProgress).getOrThrow()
            }
        }
    }

    private suspend fun transfer(
        provider: FileProvider,
        directory: String,
        source: ConflictSource,
        isDirectory: Boolean,
        resolveConflict: ConflictResolver,
        validateTarget: suspend (FileItem) -> Unit,
        copy: suspend (String) -> Unit,
    ): TransferDisposition {
        TransferCancellation.check()
        val existing = provider.listFiles(directory).getOrThrow().firstOrNull { it.name == source.name }
        if (existing != null) {
            validateTarget(existing)
            require(existing.isDirectory == isDirectory) { "A file and a folder cannot replace each other" }
            when (resolveConflict(TransferConflict(source, existing))) {
                ConflictDecision.SKIP -> return TransferDisposition.SKIPPED
                ConflictDecision.CANCEL -> throw CancellationException("Transfer cancelled")
                ConflictDecision.REPLACE -> Unit
            }
        }
        TransferCancellation.check()
        val name = if (existing == null) source.name else uniqueName(provider, directory, "stage", source.name)
        var staged: String? = null
        try {
            val created = (if (isDirectory) provider.createDirectory(directory, name) else provider.createFile(directory, name)).getOrThrow()
            staged = created.path
            check(created.name == name) { "The provider created a different destination name" }
            copy(staged)
            TransferCancellation.check()
            if (existing != null) {
                // Revalidate after the potentially long copy, before moving the user's destination.
                val current = provider.listFiles(directory).getOrThrow().firstOrNull { it.name == source.name }
                check(current != null && current.path == existing.path && current.size == existing.size &&
                    current.lastModified == existing.lastModified && current.isDirectory == existing.isDirectory) {
                    "The destination changed while copying; try again"
                }
                val backup = provider.rename(existing.path, uniqueName(provider, directory, "backup", source.name)).getOrThrow()
                try {
                    provider.rename(staged, source.name).getOrThrow()
                } catch (error: Throwable) {
                    val rollback = provider.rename(backup.path, source.name)
                    if (rollback.isFailure) {
                        throw IllegalStateException("Could not replace ${source.name}. The original remains at ${backup.path}", error).apply {
                            addSuppressed(rollback.exceptionOrNull()!!)
                        }
                    }
                    throw error
                }
                staged = null
                // Once promoted, cleanup failure leaves the committed target and backup intact.
                provider.delete(backup.path).getOrElse { error ->
                    throw IllegalStateException("Replacement saved, but the original backup could not be removed: ${backup.path}", error)
                }
            } else {
                staged = null
            }
            return TransferDisposition.COMPLETED
        } catch (error: Throwable) {
            staged?.let { path ->
                runCatching { provider.delete(path).getOrThrow() }.onFailure(error::addSuppressed)
            }
            throw error
        }
    }

    private suspend fun uniqueName(provider: FileProvider, directory: String, purpose: String, originalName: String): String {
        val names = provider.listFiles(directory).getOrThrow().mapTo(mutableSetOf()) { it.name }
        // SAF chooses the MIME type when creating the stage, so preserve a conventional extension.
        val extension = originalName.substringAfterLast('.', "").takeIf { it.length in 1..32 }?.let { ".$it" }.orEmpty()
        return generateSequence { ".voyager-$purpose-${UUID.randomUUID()}$extension" }.first { it !in names }
    }
}
