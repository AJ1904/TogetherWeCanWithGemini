package com.ajain.togetherwecanwithgemini

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.ajain.togetherwecanwithgemini.data.Screen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun BottomNavigationBar(navController: NavHostController) {

    // Retrieve the current user ID and the current route
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.systemBarsPadding().fillMaxHeight(0.09f) // Set the height and padding of the NavigationBar
    ) {
        // Define the list of navigation items with associated screens and icons
        val navItems = listOf(
            Screen.Discover to Icons.Default.Favorite,
            Screen.Learn to painterResource(id = R.drawable.baseline_school_24),
            Screen.Home to Icons.Default.Home,
            Screen.Challenges to painterResource(id = R.drawable.baseline_event_24),
            Screen.Account to Icons.Default.Person
        )

        navItems.forEach { (screen, icon) ->
            // Determine if the current item is selected
            val isSelected = currentRoute == screen.route || (screen == Screen.Account && currentRoute?.startsWith(Screen.Account.route) == true)

            NavigationBarItem(
                icon = {
                    // Display the icon based on whether it is selected or not
                    if (isSelected) {
                        Surface(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .heightIn(min = 56.dp), // Adjust the minimum height of the selected icon
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            when (icon) {
                                is ImageVector -> {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = stringResource(R.string.account),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxSize().padding(8.dp)
                                    )
                                }
                                is Painter -> {
                                    Icon(
                                        painter = icon,
                                        contentDescription = stringResource(R.string.account),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxSize().padding(8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        when (icon) {
                            is ImageVector -> {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(R.string.account),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            is Painter -> {
                                Icon(
                                    painter = icon,
                                    contentDescription = stringResource(R.string.account),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                },
                selected = isSelected,
                onClick = {
                    // Navigate to the selected screen with proper state management
                    if (screen == Screen.Account) {
                        navController.navigate("account/$userId") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
            )
        }
    }
}
