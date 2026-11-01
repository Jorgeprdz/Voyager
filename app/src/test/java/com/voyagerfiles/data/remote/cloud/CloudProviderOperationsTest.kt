package com.voyagerfiles.data.remote.cloud

import java.io.ByteArrayInputStream
import java.io.OutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CloudProviderOperationsTest {
    private val driveFile = """{"id":"file1","name":"file.txt","mimeType":"text/plain","size":"3","parents":["root"]}"""
    private class Http(val handler: (String, String, String?, ByteArray?, Map<String, String>) -> OneDriveHttpResult) : OneDriveHttp {
        override suspend fun request(method: String, url: String, bearer: String?, body: ByteArray?, mimeType: String?) = handler(method, url, bearer, body, emptyMap())
        override suspend fun requestHeaders(method: String, url: String, bearer: String?, body: ByteArray?, mimeType: String?, headers: Map<String,String>) = handler(method, url, bearer, body, headers)
        override suspend fun stream(url: String, bearer: String?, target: OutputStream, cancel: CloudCancellation, progress: (Long) -> Unit) = error("Unexpected download")
    }
    @Test fun driveListsOpaqueIdsAndServerPageToken() = runBlocking {
        val provider = GoogleDriveProvider(OneDriveTokenProvider { "authorized" }, Http { method, url, token, _, _ ->
            assertEquals("GET", method); assertEquals("authorized", token)
            assertTrue(url.contains("pageToken=opaque%2Bnext"))
            OneDriveHttpResult(200, """{"files":[$driveFile],"nextPageToken":"last"}""")
        })
        val result = provider.list("root", "opaque+next").getOrThrow()
        assertEquals("file1", result.entries.single().id); assertEquals("last", result.nextToken)
        assertEquals(CloudConnectionState.ONLINE, provider.connectionState)
    }
    @Test fun driveTrashUsesRecoverableUpdate() = runBlocking {
        var called = false
        val provider = GoogleDriveProvider(OneDriveTokenProvider { "authorized" }, Http { method, _, _, body, _ ->
            called = true; assertEquals("PATCH", method)
            assertTrue(body!!.toString(Charsets.UTF_8).contains("\"trashed\":true"))
            OneDriveHttpResult(200, "{}")
        })
        provider.trash("file1").getOrThrow(); assertTrue(called)
    }
    @Test fun graphChunkUploadCommitsOnlyOnFinalItemAndNeverLeaksBearerToSession() = runBlocking {
        val length = 5_000_000
        var parts = 0
        val progress = mutableListOf<Long>()
        val provider = OneDriveGraphProvider(OneDriveTokenProvider { "authorized" }, Http { method, url, token, body, headers ->
            if (method == "POST") {
                assertEquals("authorized", token)
                assertTrue(body!!.toString(Charsets.UTF_8).contains("\"fail\""))
                OneDriveHttpResult(200, """{"uploadUrl":"https://storage.example.com/session"}""")
            } else {
                assertEquals("PUT", method); assertNull(token); assertEquals("https://storage.example.com/session", url)
                parts++
                if (parts == 1) {
                    assertEquals("bytes 0-3276799/5000000", headers["Content-Range"])
                    OneDriveHttpResult(202, """{"nextExpectedRanges":["3276800-"]}""")
                } else OneDriveHttpResult(201, """{"id":"complete","name":"large.bin","size":5000000,"file":{"mimeType":"application/octet-stream"}}""")
            }
        })
        val result = provider.upload("root", "large.bin", "application/octet-stream", ByteArrayInputStream(ByteArray(length)), length.toLong(), CloudCancellation { false }, progress::add).getOrThrow()
        assertEquals("complete", result.id); assertEquals(2, parts)
        assertEquals(listOf(3276800L, length.toLong()), progress)
    }
    @Test fun graphDoesNotReportCopyAcceptanceAsCompletion() = runBlocking {
        val provider = OneDriveGraphProvider(OneDriveTokenProvider { "authorized" }, Http { _, _, _, _, _ -> OneDriveHttpResult(202, "{}") })
        assertTrue(provider.copy("one", "root").isFailure)
    }
    @Test fun disconnectedDriveCannotIssueRequests() = runBlocking {
        val provider = GoogleDriveProvider(OneDriveTokenProvider { "authorized" }, Http { _, _, _, _, _ -> error("Must not request") })
        provider.disconnect(); assertTrue(provider.list("root").isFailure)
        assertEquals(CloudConnectionState.DISCONNECTED, provider.connectionState)
    }
}
