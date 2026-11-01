package com.voyagerfiles.data.remote.cloud

import org.junit.Assert.*
import org.junit.Test

class OneDriveGraphPathsTest {
    @Test fun opaqueIdsAreEncodedNotUsedAsPaths() {
        assertTrue(OneDriveGraphPaths.item("abc/def").endsWith("/abc%2Fdef"))
        assertEquals("https://graph.microsoft.com/v1.0/me/drive/root",
            OneDriveGraphPaths.item("root"))
        assertTrue(OneDriveGraphPaths.upload("parent", "A B.txt").endsWith("/parent:/A%20B.txt:/content"))
    }

    @Test fun paginationMustStayOnOfficialGraphHost() {
        val good = "https://graph.microsoft.com/v1.0/me/drive/root/children?%24skiptoken=page"
        assertEquals(good, OneDriveGraphPaths.page(good))
        listOf(
            "https://evil.example/v1.0/me/drive/root",
            "http://graph.microsoft.com/v1.0/me/drive/root",
            "https://graph.microsoft.com.evil.org/v1.0/me/drive/root",
            "https://graph.microsoft.com:444/v1.0/me/drive/root",
            "https://graph.microsoft.com@evil.com/v1.0/me/drive/root",
            "https://graph.microsoft.com/v1.0/users/another/drive"
        ).forEach { url ->
            assertThrows(IllegalArgumentException::class.java) { OneDriveGraphPaths.page(url) }
        }
    }

    @Test fun pageSizeAndSearchAreBounded() {
        assertThrows(IllegalArgumentException::class.java) {
            OneDriveGraphPaths.children("root", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OneDriveGraphPaths.search("x", 201)
        }
        assertTrue(OneDriveGraphPaths.search("report", 100).contains("search(q='report')"))
    }

    @Test fun graphErrorsRemainTyped() {
        assertEquals(CloudFailure.AUTH_EXPIRED, CloudFailurePolicy.fromHttp(401))
        assertEquals(CloudFailure.RATE_LIMITED, CloudFailurePolicy.fromHttp(429))
        assertEquals(CloudFailure.SERVER_UNAVAILABLE, CloudFailurePolicy.fromHttp(503))
    }
}
