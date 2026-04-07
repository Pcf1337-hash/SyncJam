package com.syncjam.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.syncjam.app.core.ui.theme.SyncJamTheme
import com.syncjam.app.feature.auth.presentation.LoginScreen
import com.syncjam.app.feature.home.presentation.HomeScreen
import com.syncjam.app.feature.onboarding.OnboardingScreen
import com.syncjam.app.feature.session.presentation.CreateSessionScreen
import com.syncjam.app.feature.session.presentation.JoinSessionScreen
import com.syncjam.app.feature.session.presentation.SessionScreen
import com.syncjam.app.feature.settings.presentation.SettingsScreen
import com.syncjam.app.feature.voting.presentation.QueueScreen
import kotlinx.serialization.Serializable

@Serializable sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Login : Route
    @Serializable data object Home : Route
    @Serializable data class Session(val sessionId: String, val sessionCode: String = "", val isHost: Boolean = false, val displayName: String = "") : Route
    @Serializable data object CreateSession : Route
    @Serializable data class JoinSession(val code: String? = null) : Route
    @Serializable data object Queue : Route
    @Serializable data object Profile : Route
    @Serializable data object Settings : Route
}

@Composable
fun SyncJamNavGraph(
    startDestination: Route = Route.Login,
    isExpandedScreen: Boolean = false
) {
    SyncJamTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = startDestination) {

            composable<Route.Onboarding> {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Route.Login) {
                            popUpTo(Route.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            composable<Route.Settings> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable<Route.Login> {
                LoginScreen(onLoginSuccess = { navController.navigate(Route.Home) })
            }

            composable<Route.Home> {
                HomeScreen(
                    onCreateSession = { navController.navigate(Route.CreateSession) },
                    onJoinSession = { navController.navigate(Route.JoinSession()) },
                    onRejoinSession = { code, isHost ->
                        // Pop back to existing Session if it's still in the back stack (user minimized)
                        val hasSession = navController.currentBackStack.value
                            .any { it.destination.route?.contains("Session") == true }
                        if (hasSession) {
                            navController.popBackStack<Route.Session>(inclusive = false)
                        } else {
                            navController.navigate(Route.Session(sessionId = code, sessionCode = code, isHost = isHost))
                        }
                    },
                    onJoinPublicSession = { code ->
                        navController.navigate(Route.JoinSession(code = code))
                    },
                    onNavigateToSettings = { navController.navigate(Route.Settings) },
                    onNavigateToSession = {
                        // Pop back to Session if in back stack, else do nothing
                        val hasSession = navController.currentBackStack.value
                            .any { it.destination.route?.contains("Session") == true }
                        if (hasSession) {
                            navController.popBackStack<Route.Session>(inclusive = false)
                        }
                    },
                    isExpandedScreen = isExpandedScreen
                )
            }

            composable<Route.CreateSession> {
                CreateSessionScreen(
                    onSessionCreated = { id, code, displayName ->
                        navController.navigate(Route.Session(sessionId = id, sessionCode = code, isHost = true, displayName = displayName))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.JoinSession> {
                val route = it.toRoute<Route.JoinSession>()
                JoinSessionScreen(
                    onSessionJoined = { id, code, displayName ->
                        navController.navigate(Route.Session(sessionId = id, sessionCode = code, isHost = false, displayName = displayName))
                    },
                    onBack = { navController.popBackStack() },
                    initialCode = route.code
                )
            }

            composable<Route.Session> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Session>()
                SessionScreen(
                    sessionCode = route.sessionCode,
                    isHost = route.isHost,
                    displayName = route.displayName,
                    onBack = {
                        // Minimize: navigate to Home without popping Session
                        // SessionViewModel stays alive, mini player remains visible
                        navController.navigate(Route.Home) {
                            launchSingleTop = false
                        }
                    },
                    onLeave = {
                        // Actually leave: pop Session from back stack and go to Home
                        navController.navigate(Route.Home) {
                            popUpTo(Route.Home) { inclusive = false }
                        }
                    },
                    onOpenPlaylist = { navController.navigate(Route.Queue) }
                )
            }

            composable<Route.Queue> {
                val sessionEntry = remember(it) {
                    try { navController.getBackStackEntry<Route.Session>() } catch (e: Exception) { null }
                }
                QueueScreen(
                    onBack = { navController.popBackStack() },
                    sessionEntry = sessionEntry
                )
            }

            composable<Route.Profile> {
                // Profile is a tab inside HomeScreen — navigate back to Home
                navController.navigate(Route.Home) {
                    popUpTo(Route.Home) { inclusive = false }
                }
            }
        }
    }
}
