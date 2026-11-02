package com.nova.a12tester.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigation routes for the TestPilot app. */
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SIGN_IN = "sign_in"
    const val HOME = "home"
    const val TEAMS = "teams"
    const val FEATURES = "features"
    const val DISCOVER = "features"
    const val SUBMIT_APP = "submit_app"
    const val APP_DETAILS = "app_details/{appId}"
    const val MY_APPS = "my_apps"
    const val ACTIVE_ASSIGNMENTS = "active_assignments"
    const val COMPLETED_TASKS = "completed_tasks"
    const val TESTING_INSTRUCTIONS = "testing_instructions/{assignmentId}"
    const val TESTING_PROGRESS = "testing_progress/{assignmentId}"
    const val FEEDBACK = "feedback/{assignmentId}"
    const val COIN_WALLET = "coin_wallet"
    const val REWARDED_ADS = "rewarded_ads"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val USER_PROFILE = "user_profile/{userId}"
    const val SETTINGS = "settings"
    const val HOW_TO_USE = "how_to_use"
    const val PREMIUM_SUBSCRIPTION = "premium_subscription"
    const val LEADERBOARD = "leaderboard"
    const val CHAT = "chat/{appId}/{assignmentId}/{receiverId}"
    const val REPORT_APP = "report_app/{appId}"
    const val ADMIN_DASHBOARD = "admin_dashboard"

    fun appDetails(appId: String) = "app_details/$appId"
    fun instructions(assignmentId: String) = "testing_instructions/$assignmentId"
    fun progress(assignmentId: String) = "testing_progress/$assignmentId"
    fun feedback(assignmentId: String) = "feedback/$assignmentId"
    fun reportApp(appId: String) = "report_app/$appId"
    fun userProfile(userId: String) = "user_profile/$userId"
    fun chat(appId: String, assignmentId: String, receiverId: String) = "chat/$appId/$assignmentId/$receiverId"
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.TEAMS, "Teams", Icons.Filled.Groups),
    BottomNavItem(Routes.ACTIVE_ASSIGNMENTS, "Testing", Icons.AutoMirrored.Filled.Assignment),
    BottomNavItem(Routes.COIN_WALLET, "Wallet", Icons.Filled.AccountBalanceWallet),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person)
)
