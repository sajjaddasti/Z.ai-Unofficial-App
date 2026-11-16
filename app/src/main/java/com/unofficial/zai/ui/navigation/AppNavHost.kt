package com.unofficial.zai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.unofficial.zai.ui.screens.SettingsScreen
import com.unofficial.zai.ui.screens.WebScreen
import com.unofficial.zai.ui.theme.ThemeMode
import com.unofficial.zai.ui.web.WebViewModel

@Composable
fun AppNavHost(
    vm: WebViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "web") {
        composable("web") {
            WebScreen(
                navController = navController,
                vm = vm
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                vm = vm,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
    }
}
