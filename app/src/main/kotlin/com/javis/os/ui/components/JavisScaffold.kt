package com.javis.os.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.javis.os.ui.navigation.Screen
import com.javis.os.ui.theme.BackgroundDark
import com.javis.os.ui.theme.CyanPrimary
import com.javis.os.ui.theme.SurfaceDark

data class NavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@Composable
fun JavisScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val navItems = listOf(
        NavItem(Screen.Voice, Icons.Default.Mic, "Voice"),
        NavItem(Screen.Chat, Icons.Default.Chat, "Chat"),
        NavItem(Screen.Memory, Icons.Default.Memory, "Memory"),
        NavItem(Screen.Notifications, Icons.Default.Notifications, "Alerts"),
        NavItem(Screen.Settings, Icons.Default.Settings, "Settings"),
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Animated cyan glow for nav bar
    val infiniteTransition = rememberInfiniteTransition(label = "navGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = CyanPrimary.copy(alpha = glowAlpha),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f
                    )
                }
            ) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.screen.route,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanPrimary,
                            selectedTextColor = CyanPrimary,
                            indicatorColor = CyanPrimary.copy(alpha = 0.15f),
                            unselectedIconColor = Color(0xFF546E7A),
                            unselectedTextColor = Color(0xFF546E7A)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundDark,
                            Color(0xFF0A1628)
                        )
                    )
                )
        ) {
            content(paddingValues)
        }
    }
}
