package com.voyagerfiles.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.File
import java.security.MessageDigest

class IconResourceContractTest {

    @Test
    fun adaptiveIconsUseDedicatedMonochromeArtwork() {
        val repositoryRoot = repositoryRoot()
        val monochrome = File(repositoryRoot, "app/src/main/res/drawable/ic_launcher_monochrome.xml")
        assertTrue("Dedicated monochrome icon is missing", monochrome.isFile)

        listOf("ic_launcher.xml", "ic_launcher_round.xml").forEach { name ->
            val adaptiveIcon = File(repositoryRoot, "app/src/main/res/mipmap-anydpi-v26/$name").readText()
            assertTrue("$name must use the dedicated monochrome icon", adaptiveIcon.contains("@drawable/ic_launcher_monochrome"))
        }
    }

    @Test
    fun approvedLauncherAndStoreAssetsAreInstalledExactly() {
        val repositoryRoot = repositoryRoot()
        approvedGitBlobs.forEach { (path, expectedHash) ->
            val file = File(repositoryRoot, path)
            assertTrue("Approved icon asset is missing: $path", file.isFile)
            assertEquals("Approved icon asset changed: $path", expectedHash, gitBlobIdentity(file))
        }
    }

    @Test
    fun rasterFallbacksAndStoreIconHaveRequiredDimensions() {
        val repositoryRoot = repositoryRoot()
        val densities = mapOf(
            "mdpi" to 48,
            "hdpi" to 72,
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192,
        )
        densities.forEach { (density, expectedSize) ->
            listOf("ic_launcher.png", "ic_launcher_round.png").forEach { name ->
                assertSquarePng(
                    file = File(repositoryRoot, "app/src/main/res/mipmap-$density/$name"),
                    expectedSize = expectedSize,
                )
            }
        }
        assertSquarePng(
            file = File(repositoryRoot, "fastlane/metadata/android/en-US/images/icon.png"),
            expectedSize = 512,
        )
    }

    private fun assertSquarePng(file: File, expectedSize: Int) {
        val (width, height) = DataInputStream(file.inputStream()).use { input ->
            assertEquals("Unexpected PNG header for ${file.path}", 16, input.skipBytes(16))
            input.readInt() to input.readInt()
        }
        assertEquals("Unexpected width for ${file.path}", expectedSize, width)
        assertEquals("Unexpected height for ${file.path}", expectedSize, height)
    }

    private fun repositoryRoot(): File {
        val workingDirectory = File(checkNotNull(System.getProperty("user.dir")))
        return listOf(workingDirectory, checkNotNull(workingDirectory.parentFile))
            .first { File(it, "app/src/main/res").isDirectory && File(it, "fastlane").isDirectory }
    }

    /** Git blob identity of the artwork explicitly approved at 77631b0.
     * SHA-1 is calculated over "blob <byte-length>\\0" plus file bytes, as Git does;
     * this accepts no substitutions from Voyager or later redesign phases.
     */
    private fun gitBlobIdentity(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${file.length()}\u0000".toByteArray(Charsets.UTF_8))
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                if (size > 0) digest.update(buffer, 0, size)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private companion object {
        private val approvedGitBlobs = mapOf(
            "app/src/main/res/drawable-nodpi/finderdex_icon_art.webp" to "34c8d844abeaec4d881ae6ea05fe03e945971d82",
            "app/src/main/res/drawable/ic_launcher_background.xml" to "119da5ba310cd901c2666decc1f87ede02659f1e",
            "app/src/main/res/drawable/ic_launcher_foreground.xml" to "ba9e62ea9a4b0fd71ba43cbaf3fe5fe851270a61",
            "app/src/main/res/drawable/ic_launcher_monochrome.xml" to "5d584bcaf23ee158f79830eed1c68c2ee604f01b",
            "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml" to "8fde45638ac86d7eaeea3d6cc170ab0bf8587dd4",
            "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml" to "8fde45638ac86d7eaeea3d6cc170ab0bf8587dd4",
            "app/src/main/res/mipmap-hdpi/ic_launcher_round.png" to "4a65a314c11e397aae892596d80bd8c424136fee",
            "app/src/main/res/mipmap-hdpi/ic_launcher.png" to "29509932831176be8e1f6ba070c9534ff4d86f87",
            "app/src/main/res/mipmap-mdpi/ic_launcher_round.png" to "f21d438a452add4884ddf434155ac8ef1b76f7ed",
            "app/src/main/res/mipmap-mdpi/ic_launcher.png" to "194fafd5e380b4e44aff06c959cbf2e9df1b4d01",
            "app/src/main/res/mipmap-xhdpi/ic_launcher_round.png" to "1a6d42d9b6a0df1175c908c5f905b7b2ff06d058",
            "app/src/main/res/mipmap-xhdpi/ic_launcher.png" to "8057d813af5c41a6257d89bbf7265f5c7aa861f7",
            "app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png" to "6d21049ac72dcd06c00e6b5881e9271fab687946",
            "app/src/main/res/mipmap-xxhdpi/ic_launcher.png" to "a63c7d32f611c23b606e21f8bba5b766cd21331b",
            "app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" to "cd750c25766d7be480cf8460e06cd42e96a80b98",
            "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" to "423355fac8a56021e8dcc6e9927410f6f2bbc91c",
            "fastlane/metadata/android/en-US/images/icon.png" to "f00f2fa99cbcafc1bcd625107351539436a16a27",
        )
    }
}
