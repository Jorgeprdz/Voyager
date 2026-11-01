package com.voyagerfiles.data.repository

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.util.UUID

/** Never replace a distinct entry. Case-only changes use an exclusive temporary sibling. */
internal fun renameLocalWithoutOverwrite(source: File, name: String): File {
    require(name.isNotBlank() && name !in listOf(".", "..") &&
        name.none { it == '/' || it == '\\' || it.isISOControl() } && name.toByteArray().size <= 255)
    val target = File(source.parentFile, name)
    if (source.name == name) return source
    TransferCancellation.check()
    val exists = Files.exists(target.toPath(), NOFOLLOW_LINKS)
    val caseOnly = source.name.equals(name, ignoreCase = true) && exists &&
        !Files.isSymbolicLink(target.toPath()) && Files.isSameFile(source.toPath(), target.toPath())
    check(!exists || caseOnly) { "$name already exists in this folder" }
    if (!caseOnly) Files.move(source.toPath(), target.toPath())
    else {
        val temporary = File(source.parentFile, ".finderdex-rename-${UUID.randomUUID()}")
        Files.move(source.toPath(), temporary.toPath())
        try { Files.move(temporary.toPath(), target.toPath()) }
        catch (failure: Exception) {
            try { Files.move(temporary.toPath(), source.toPath()) }
            catch (rollback: Exception) { failure.addSuppressed(rollback) }
            throw failure
        }
    }
    check(Files.exists(target.toPath(), NOFOLLOW_LINKS)) { "Rename could not be verified" }
    return target
}
