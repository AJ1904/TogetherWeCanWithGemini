package com.ajain.togetherwecanwithgemini.data

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Discover : Screen("discover")
    object Settings : Screen("settings")
    object Learn : Screen("learn")
    object Account: Screen("account")
    object Challenges: Screen("challenges")
}