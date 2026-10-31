package com.voyagerfiles.data.remote.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CloudProviderContractTest {
    @Test fun paginationCompletesOnLastPage() = runBlocking {
        val requested = mutableListOf<String?>()
        val files = CloudPageCollector.collect { token ->
            requested.add(token)
            when (token) {
                null -> CloudPage(listOf(entry("one")), "next")
                "next" -> CloudPage(listOf(entry("two")))
                else -> error("unexpected token")
            }
        }
        assertEquals(listOf(null, "next"), requested)
        assertEquals(listOf("one", "two"), files.map { it.id })
    }

    @Test fun paginationRejectsRepeatedTokensAndUnboundedLists() = runBlocking {
        assertThrows(IllegalStateException::class.java) {
            runBlocking { CloudPageCollector.collect { CloudPage(emptyList(), "loop") } }
        }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { CloudPageCollector.collect(maxPages = 2) { CloudPage(emptyList(), "new-${System.nanoTime()}") } }
        }
    }

    @Test fun mapsAuthorizationQuotaConflictsAndUnavailable() {
        assertEquals(CloudFailure.AUTH_EXPIRED, CloudFailurePolicy.fromHttp(401))
        assertEquals(CloudFailure.ACCESS_DENIED, CloudFailurePolicy.fromHttp(403))
        assertEquals(CloudFailure.NOT_FOUND, CloudFailurePolicy.fromHttp(404))
        assertEquals(CloudFailure.CONFLICT, CloudFailurePolicy.fromHttp(409))
        assertEquals(CloudFailure.CONFLICT, CloudFailurePolicy.fromHttp(412))
        assertEquals(CloudFailure.RATE_LIMITED, CloudFailurePolicy.fromHttp(429))
        assertEquals(CloudFailure.SERVER_UNAVAILABLE, CloudFailurePolicy.fromHttp(503))
        assertFalse(CloudFailurePolicy.canRetry(CloudFailure.AUTH_EXPIRED, 0))
        assertFalse(CloudFailurePolicy.canRetry(CloudFailure.CONFLICT, 0))
        assertTrue(CloudFailurePolicy.canRetry(CloudFailure.RATE_LIMITED, 2))
        assertFalse(CloudFailurePolicy.canRetry(CloudFailure.RATE_LIMITED, 4))
    }

    @Test fun backsOffSafelyAndHonorsRetryAfter() {
        assertEquals(1000L, CloudFailurePolicy.retryDelayMillis(0))
        assertEquals(4000L, CloudFailurePolicy.retryDelayMillis(2))
        assertEquals(60000L, CloudFailurePolicy.retryDelayMillis(20))
        assertEquals(120000L, CloudFailurePolicy.retryDelayMillis(0, 1000L))
    }

    @Test fun nativeWorkspaceDocumentsRequireExport() {
        assertTrue(CloudDocumentPolicy.requiresExport("application/vnd.google-apps.document"))
        assertFalse(CloudDocumentPolicy.requiresExport("application/vnd.google-apps.folder"))
        assertEquals("application/pdf", CloudDocumentPolicy.exportMime("application/vnd.google-apps.presentation"))
        assertNull(CloudDocumentPolicy.exportMime("application/vnd.google-apps.form"))
    }

    private fun entry(id: String) = CloudEntry(id, "root", id, "text/plain", false)
}
