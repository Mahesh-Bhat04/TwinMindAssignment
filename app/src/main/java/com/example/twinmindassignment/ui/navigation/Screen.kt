package com.example.twinmindassignment.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Recording : Screen("recording/{sessionId}") {
        fun createRoute(sessionId: String) = "recording/$sessionId"
    }
    data object MeetingDetail : Screen("meeting/{sessionId}") {
        fun createRoute(sessionId: String) = "meeting/$sessionId"
    }
}
