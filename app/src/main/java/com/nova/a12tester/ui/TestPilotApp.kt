package com.nova.a12tester.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.nova.a12tester.data.TestPilotRepository
import com.nova.a12tester.data.TestPilotViewModel
import com.nova.a12tester.ui.screens.*

@Composable
fun TestPilotApp(vm: TestPilotViewModel) {
    val navController = rememberNavController()
    val state by TestPilotRepository.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = remember {
        setOf(Routes.HOME, Routes.DISCOVER, Routes.ACTIVE_ASSIGNMENTS, Routes.COIN_WALLET, Routes.PROFILE)
    }
    val showBottomBar = currentRoute in bottomBarRoutes

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            TestPilotRepository.consumeMessages()
        }
    }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            TestPilotRepository.consumeMessages()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.ui.graphics.Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            ),
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onReady = {
                        val destination = when {
                            state.currentUser == null -> Routes.SIGN_IN
                            !state.hasCompletedOnboarding -> Routes.ONBOARDING
                            else -> Routes.HOME
                        }
                        navController.navigate(destination) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SIGN_IN) {
                SignInScreen(
                    onSignedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SIGN_IN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onOpenDiscover = { navController.navigate(Routes.DISCOVER) },
                    onOpenSubmit = { navController.navigate(Routes.SUBMIT_APP) },
                    onOpenWallet = { navController.navigate(Routes.COIN_WALLET) },
                    onOpenAssignment = { assignmentId -> navController.navigate(Routes.progress(assignmentId)) },
                    onOpenMyApps = { navController.navigate(Routes.MY_APPS) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                    onOpenApp = { appId -> navController.navigate(Routes.appDetails(appId)) }
                )
            }

            composable(Routes.TEAMS) {
                TeamsScreen(
                    onOpenSubmit = { navController.navigate(Routes.SUBMIT_APP) },
                    onOpenWallet = { navController.navigate(Routes.COIN_WALLET) },
                    onOpenAssignment = { assignmentId -> navController.navigate(Routes.progress(assignmentId)) },
                    onOpenApp = { appId -> navController.navigate(Routes.appDetails(appId)) }
                )
            }

            composable(Routes.FEATURES) {
                FeaturesScreen(
                    onOpenSubmit = { navController.navigate(Routes.SUBMIT_APP) },
                    onOpenWallet = { navController.navigate(Routes.COIN_WALLET) },
                    onOpenTesting = { navController.navigate(Routes.ACTIVE_ASSIGNMENTS) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MY_APPS) {
                MyAppsScreen(
                    vm = vm,
                    onOpenApp = { appId -> navController.navigate(Routes.appDetails(appId)) },
                    onSubmitNew = { navController.navigate(Routes.SUBMIT_APP) }
                )
            }

            composable(Routes.ACTIVE_ASSIGNMENTS) {
                ActiveAssignmentsScreen(
                    vm = vm,
                    onOpenAssignment = { assignmentId -> navController.navigate(Routes.progress(assignmentId)) },
                    onFindMoreApps = { navController.navigate(Routes.DISCOVER) }
                )
            }

            composable(Routes.SUBMIT_APP) {
                SubmitAppScreen(
                    vm = vm,
                    onSubmitted = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.APP_DETAILS,
                arguments = listOf(navArgument("appId") { type = NavType.StringType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId").orEmpty()
                AppDetailsScreen(
                    appId = appId,
                    vm = vm,
                    onOpenProgress = { assignmentId ->
                        navController.navigate(Routes.instructions(assignmentId))
                    },
                    onOpenChat = { aId, assignId, receiverId ->
                        navController.navigate(Routes.chat(aId, assignId, receiverId))
                    }
                )
            }

            composable(
                route = Routes.TESTING_INSTRUCTIONS,
                arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val assignmentId = backStackEntry.arguments?.getString("assignmentId").orEmpty()
                TestingInstructionsScreen(
                    assignmentId = assignmentId,
                    vm = vm,
                    onDone = {
                        navController.navigate(Routes.progress(assignmentId)) {
                            popUpTo(Routes.TESTING_INSTRUCTIONS) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.TESTING_PROGRESS,
                arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val assignmentId = backStackEntry.arguments?.getString("assignmentId").orEmpty()
                TestingProgressScreen(
                    assignmentId = assignmentId,
                    vm = vm,
                    onOpenFeedback = { aId ->
                        navController.navigate(Routes.feedback(aId))
                    },
                    onOpenChat = { appId, assignId, receiverId ->
                        navController.navigate(Routes.chat(appId, assignId, receiverId))
                    }
                )
            }

            composable(
                route = Routes.FEEDBACK,
                arguments = listOf(navArgument("assignmentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val assignmentId = backStackEntry.arguments?.getString("assignmentId").orEmpty()
                FeedbackScreen(
                    assignmentId = assignmentId,
                    vm = vm,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.COIN_WALLET) {
                CoinWalletScreen(
                    onOpenAds = { navController.navigate(Routes.REWARDED_ADS) }
                )
            }

            composable(Routes.REWARDED_ADS) {
                RewardedAdsScreen(
                    vm = vm,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    vm = vm,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    vm = vm,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN_DASHBOARD) },
                    onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                    onOpenHowToUse = { navController.navigate(Routes.HOW_TO_USE) },
                    onOpenSubmit = { navController.navigate(Routes.SUBMIT_APP) },
                    onOpenMyApps = { navController.navigate(Routes.MY_APPS) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onOpenFeatures = { navController.navigate(Routes.FEATURES) },
                    onSignedOut = {
                        navController.navigate(Routes.SIGN_IN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Routes.HOW_TO_USE) {
                HowToUseScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PREMIUM_SUBSCRIPTION) {
                PremiumSubscriptionScreen(vm = vm)
            }

            composable(
                route = Routes.REPORT_APP,
                arguments = listOf(navArgument("appId") { type = NavType.StringType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId").orEmpty()
                ReportAppScreen(
                    appId = appId,
                    vm = vm,
                    onDone = { navController.popBackStack() }
                )
            }

            composable(Routes.ADMIN_DASHBOARD) {
                if (state.isAdmin) {
                    AdminDashboardScreen(vm = vm)
                } else {
                    // Defense-in-depth: bounce non-admins away from the admin route
                    // (deep links / manual navigation). The screen self-gates too, and
                    // Firestore rules independently enforce the `admin` token claim.
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            composable(Routes.COMPLETED_TASKS) {
                CompletedTasksScreen()
            }

            composable(Routes.LEADERBOARD) {
                LeaderboardScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.CHAT,
                arguments = listOf(
                    navArgument("appId") { type = NavType.StringType },
                    navArgument("assignmentId") { type = NavType.StringType },
                    navArgument("receiverId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId").orEmpty()
                val assignmentId = backStackEntry.arguments?.getString("assignmentId").orEmpty()
                val receiverId = backStackEntry.arguments?.getString("receiverId").orEmpty()
                ChatScreen(
                    appId = appId,
                    assignmentId = assignmentId,
                    receiverId = receiverId,
                    vm = vm,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId").orEmpty()
                UserProfileScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAdmin = { navController.navigate(Routes.ADMIN_DASHBOARD) }
                )
            }
        }
    }
}
