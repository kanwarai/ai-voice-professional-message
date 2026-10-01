package com.kanwarai.voiceprofessionalmessage.navigation

sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object Result : AppRoute("result")
    data object History : AppRoute("history")
    data object Settings : AppRoute("settings")
}
