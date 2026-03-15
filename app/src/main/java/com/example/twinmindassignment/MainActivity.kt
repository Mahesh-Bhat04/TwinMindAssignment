package com.example.twinmindassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.twinmindassignment.ui.navigation.TwinMindNavHost
import com.example.twinmindassignment.ui.theme.TwinMindAssignmentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwinMindAssignmentTheme {
                val navController = rememberNavController()
                TwinMindNavHost(navController = navController)
            }
        }
    }
}
