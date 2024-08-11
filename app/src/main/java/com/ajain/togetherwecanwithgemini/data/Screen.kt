package com.ajain.togetherwecanwithgemini.data

// Sealed class representing different screen routes in the app's navigation system.
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Discover : Screen("discover")
    object Settings : Screen("settings")
    object Learn : Screen("learn")
    object Account: Screen("account")
    object Challenges: Screen("challenges")
}