package com.kanwarai.voiceprofessionalmessage.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kanwarai.voiceprofessionalmessage.ui.screens.HistoryScreen
import com.kanwarai.voiceprofessionalmessage.ui.screens.HomeScreen
import com.kanwarai.voiceprofessionalmessage.ui.screens.ResultScreen
import com.kanwarai.voiceprofessionalmessage.ui.screens.SettingsScreen
import com.kanwarai.voiceprofessionalmessage.ui.theme.ThemeMode

@Composable
fun AppNavigation(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = modifier,
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                onOpenResult = { navController.navigateSingleTop(AppRoute.Result) },
                onOpenHistory = { navController.navigateSingleTop(AppRoute.History) },
                onOpenSettings = { navController.navigateSingleTop(AppRoute.Settings) },
            )
        }
        composable(AppRoute.Result.route) {
            ResultScreen(onBack = navController::navigateUp)
        }
        composable(AppRoute.History.route) {
            HistoryScreen(onBack = navController::navigateUp)
        }
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                onBack = navController::navigateUp,
            )
        }
    }
}

private fun NavHostController.navigateSingleTop(destination: AppRoute) {
    navigate(destination.route) {
        launchSingleTop = true
    }
}
