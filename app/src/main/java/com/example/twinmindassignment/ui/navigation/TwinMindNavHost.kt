package com.example.twinmindassignment.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.twinmindassignment.ui.dashboard.DashboardScreen
import com.example.twinmindassignment.ui.detail.MeetingDetailScreen
import com.example.twinmindassignment.ui.recording.RecordingScreen

@Composable
fun TwinMindNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRecording = { sessionId ->
                    navController.navigate(Screen.Recording.createRoute(sessionId))
                },
                onNavigateToDetail = { sessionId ->
                    navController.navigate(Screen.MeetingDetail.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.Recording.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            RecordingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { sessionId ->
                    navController.popBackStack()
                    navController.navigate(Screen.MeetingDetail.createRoute(sessionId))
                }
            )
        }

        composable(
            route = Screen.MeetingDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            MeetingDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
