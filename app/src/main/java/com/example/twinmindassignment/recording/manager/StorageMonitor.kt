package com.example.twinmindassignment.recording.manager

import android.content.Context
import android.os.StatFs
import com.example.twinmindassignment.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StorageMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getAvailableStorage(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBytes
    }

    fun isStorageLow(): Boolean = getAvailableStorage() < Constants.MIN_STORAGE_BYTES

    fun isStorageWarning(): Boolean = getAvailableStorage() < Constants.WARNING_STORAGE_BYTES
}
