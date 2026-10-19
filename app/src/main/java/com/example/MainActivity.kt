package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.model.NetworkStatus
import com.example.core.network.NetworkMonitor
import com.example.core.security.UserSessionManager
import com.example.data.preferences.NilaPreferences
import com.example.data.preferences.NilaUserSettings
import com.example.feature.camera.CameraScreen
import com.example.feature.chat.ChatScreen
import com.example.feature.control.ControlScreen
import com.example.feature.creation.CreateScreen
import com.example.feature.history.HistoryScreen
import com.example.feature.home.HomeScreen
import com.example.feature.memory.MemoryScreen
import com.example.feature.onboarding.OnboardingScreen
import com.example.feature.settings.SettingsScreen
import com.example.feature.study.StudyScreen
import com.example.ui.components.NilaBottomNavigation
import com.example.ui.theme.NilaAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val networkMonitor = NetworkMonitor(this)
        val preferences = NilaPreferences(this)
        val userSessionManager = UserSessionManager.getInstance(this)

        setContent {
            val networkStatus by networkMonitor.networkStatus.collectAsStateWithLifecycle(
                initialValue = NetworkStatus.ONLINE
            )
            val settings by preferences.settingsFlow.collectAsStateWithLifecycle(
                initialValue = NilaUserSettings()
            )

            NilaAiTheme(
                themeMode = settings.themeMode,
                useDynamicColor = false
            ) {
                var isFirstLaunch by remember(settings.isOnboardingCompleted) {
                    mutableStateOf(!settings.isOnboardingCompleted)
                }

                if (isFirstLaunch) {
                    OnboardingScreen(
                        onComplete = { isFirstLaunch = false }
                    )
                } else {
                    NilaAppScaffold(
                        networkStatus = networkStatus,
                        settings = settings,
                        userSessionManager = userSessionManager
                    )
                }
            }
        }
    }
}

@Composable
fun NilaAppScaffold(
    networkStatus: NetworkStatus,
    settings: NilaUserSettings,
    userSessionManager: UserSessionManager
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val primaryRoutes = setOf("home", "chat", "camera", "create", "study", "settings")
    val shouldShowBottomBar = currentRoute in primaryRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                NilaBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo("home") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    networkStatus = networkStatus,
                    settings = settings,
                    onNavigate = { target -> navController.navigate(target) },
                    userSessionManager = userSessionManager
                )
            }
            composable("chat") {
                ChatScreen(
                    networkStatus = networkStatus,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("camera") {
                CameraScreen(
                    networkStatus = networkStatus,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("create") {
                CreateScreen(
                    networkStatus = networkStatus,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("study") {
                StudyScreen(
                    networkStatus = networkStatus,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("memory") {
                MemoryScreen(
                    networkStatus = networkStatus,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("control") {
                ControlScreen(
                    networkStatus = networkStatus,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("history") {
                HistoryScreen(
                    networkStatus = networkStatus,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    networkStatus = networkStatus,
                    currentSettings = settings,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
