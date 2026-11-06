package com.voyagerfiles.util
import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test
class DocumentPathPolicyTest {
 @Test fun rejectsTraversalAndSymlinkEscape() {
  val root = Files.createTempDirectory("finder-root").toFile()
  val other = Files.createTempDirectory("finder-other").toFile()
  try { File(root, "ok").writeText("ok"); assertEquals(File(root,"ok").canonicalFile, DocumentPathPolicy.resolve(root,"ok"));
   for (path in listOf("../outside", "/etc/passwd", "a/../../escape")) assertTrue(runCatching { DocumentPathPolicy.resolve(root,path) }.isFailure)
   Files.createSymbolicLink(File(root,"link").toPath(),other.toPath()); assertTrue(runCatching { DocumentPathPolicy.resolve(root,"link/file") }.isFailure)
  } finally { File(root,"link").delete(); root.deleteRecursively(); other.deleteRecursively() }
 }
 @Test fun protectedAndroidTreesAreNotReexportedWithDifferentCase() {
  val root = Files.createTempDirectory("finder-protected").toFile()
  try {
   for (path in listOf("Android/data/app", "android/DATA/app", "ANDROID/OBB/app"))
    assertTrue(path, runCatching { DocumentPathPolicy.resolve(root, path) }.isFailure)
  } finally { root.deleteRecursively() }
 }
 @Test fun creationRejectsPathsAndReservedNames() { for(name in listOf("", ".", "..", "a/b", "a\\b", "a\u0000b")) assertFalse(DocumentPathPolicy.validName(name)); assertTrue(DocumentPathPolicy.validName("fotos 2026.zip")) }
}
