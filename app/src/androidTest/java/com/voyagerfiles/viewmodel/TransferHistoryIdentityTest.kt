package com.voyagerfiles.viewmodel

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.voyagerfiles.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class TransferHistoryIdentityTest {
    @Test fun receiverIdentitySurvivesJournalReload() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val name = "transfer-identity-test-${UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(ignored: String, mode: Int): SharedPreferences =
                base.getSharedPreferences(name, mode)
        }
        try {
            val result = OperationResult(TransferProgress(UiText.Dynamic("Receive")),
                OperationOutcome.COMPLETED, id = 123456789L)
            AndroidTransferHistoryStore(context).finished(listOf(result))
            val restored = AndroidTransferHistoryStore(context).restore().single()
            assertEquals(result.id, restored.id)
            assertEquals(result.outcome, restored.outcome)
        } finally { base.deleteSharedPreferences(name) }
    }
}
