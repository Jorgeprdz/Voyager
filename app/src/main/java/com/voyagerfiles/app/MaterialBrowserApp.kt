package com.voyagerfiles.app

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.voyagerfiles.viewmodel.FileBrowserViewModel
import com.voyagerfiles.viewmodel.TransferOperationController
import com.voyagerfiles.viewmodel.AndroidTransferHistoryStore

class VoyagerApp : Application(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    val browserViewModel: FileBrowserViewModel
        get() = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(this))[FileBrowserViewModel::class.java]
    // Consumed synchronously by launch(); service owns these temporary grants until it stops.
    internal var transferSourceGrants: List<android.net.Uri> = emptyList()
    val transfers by lazy {
        TransferOperationController(startForeground = {
            val serviceIntent = Intent(this, TransferService::class.java)
            transferSourceGrants.firstOrNull()?.let { first ->
                serviceIntent.clipData = android.content.ClipData.newRawUri("FinderDex transfer", first).apply {
                    transferSourceGrants.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
                }
                serviceIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }, historyStore = AndroidTransferHistoryStore(this))
    }
}
