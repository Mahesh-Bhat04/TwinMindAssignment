package com.example.twinmindassignment.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable

@Composable
fun getRequiredPermissions(): List<String> {
    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return permissions
}
