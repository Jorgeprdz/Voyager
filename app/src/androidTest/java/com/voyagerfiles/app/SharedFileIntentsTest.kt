package com.voyagerfiles.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test

class SharedFileIntentsTest {
    @Test fun acceptsSingleAndMultipleContentStreams() {
        val first = Uri.parse("content://example/a")
        val second = Uri.parse("content://example/b")
        assertEquals(listOf(first), SharedFileIntents.incoming(Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, first)))
        assertEquals(listOf(first, second), SharedFileIntents.incoming(Intent(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(first, second))))
    }
    @Test fun rejectsFileUrisAndOversizedBatches() {
        assertTrue(SharedFileIntents.incoming(Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/a"))).isEmpty())
        assertTrue(SharedFileIntents.incoming(Intent(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                ArrayList((1..65).map { Uri.parse("content://example/$it") }))).isEmpty())
        assertTrue(SharedFileIntents.incoming(Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("content://example/a"))).isEmpty())
    }
    @Test fun malformedExtrasAreRejectedWithoutCrashing() {
        assertTrue(SharedFileIntents.incoming(Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_STREAM, "not a parcelable URI")).isEmpty())
    }
    @Test fun supportsClipDataFallback() {
        val uri = Uri.parse("content://example/a")
        val source = Intent(Intent.ACTION_SEND).apply { clipData = ClipData.newRawUri("file", uri) }
        assertEquals(listOf(uri), SharedFileIntents.incoming(source))
    }
}
