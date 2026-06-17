package com.javis.os.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.javis.os.ui.screens.ChatScreen
import com.javis.os.ui.screens.MemoryScreen
import com.javis.os.ui.screens.NotificationsScreen
import com.javis.os.ui.screens.SettingsScreen
import com.javis.os.ui.screens.VoiceScreen
import com.javis.os.ui.components.JavisScaffold

sealed class Screen(val route: String) {
    object Voice : Screen("voice")
    object Chat : Screen("chat")
    object Memory : Screen("memory")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
}

@Composable
fun JavisNavGraph(
    navController: NavHostController = rememberNavController()
) {
    JavisScaffold(navController = navController) {
        NavHost(
            navController = navController,
            startDestination = Screen.Voice.route
        ) {
            composable(Screen.Voice.route) {
                VoiceScreen(navController = navController)
            }
            composable(Screen.Chat.route) {
                ChatScreen(navController = navController)
            }
            composable(Screen.Memory.route) {
                MemoryScreen(navController = navController)
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
