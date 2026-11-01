package com.voyagerfiles.data.remote.cloud

import org.junit.Assert.*
import org.junit.Test

class CloudUploadPolicyTest {
    @Test fun chunkSizesRespectEachServiceAlignment() {
        assertEquals(0, CloudUploadPolicy.DRIVE_CHUNK % 262144)
        assertEquals(0, CloudUploadPolicy.CHUNK % 327680)
    }
    @Test fun rejectsCredentialLeakAndPlaintextSessionUrls() {
        assertFalse(CloudUploadPolicy.safeSession("http://example.com/upload"))
        assertFalse(CloudUploadPolicy.safeSession("https://user:pass@example.com/upload"))
        assertTrue(CloudUploadPolicy.safeSession("https://storage.example.com/upload?sig=opaque"))
    }
    @Test fun resumesOnlyAtServerConfirmedOffset() {
        assertEquals(327680L, CloudUploadPolicy.nextOffset(listOf("327680-"), 655360))
        assertEquals(0L, CloudUploadPolicy.nextOffset(listOf("0-327679"), 655360))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOffsetBeyondSource() {
        CloudUploadPolicy.nextOffset(listOf("999-"), 32)
    }
}
