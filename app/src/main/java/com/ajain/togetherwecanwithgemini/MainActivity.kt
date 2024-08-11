package com.ajain.togetherwecanwithgemini

import com.ajain.togetherwecanwithgemini.AccountScreen
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ajain.togetherwecanwithgemini.data.Goal
import com.ajain.togetherwecanwithgemini.data.SDG
import com.ajain.togetherwecanwithgemini.data.Screen
import com.ajain.togetherwecanwithgemini.goals.GoalDetailActivity
import com.ajain.togetherwecanwithgemini.ui.theme.TogetherWeCanWithGeminiTheme
import com.ajain.togetherwecanwithgemini.viewmodels.MainViewModel
import com.firebase.ui.auth.AuthUI
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize
import com.ajain.togetherwecanwithgemini.data.AppConfig
import com.ajain.togetherwecanwithgemini.data.Summary
import com.ajain.togetherwecanwithgemini.data.SummaryWithLanguage
import com.ajain.togetherwecanwithgemini.utils.HtmlTextView
import com.ajain.togetherwecanwithgemini.utils.markdownToHtml
import java.util.Locale
import com.ajain.togetherwecanwithgemini.viewmodels.ChallengesViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val challengesViewModel: ChallengesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up window insets for edge-to-edge experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Initialize Firebase and configure App Check
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )

        // Set the device locale
        AppConfig.deviceLocale = getDeviceLocale(this)

        // Set up the content with the main theme and MainScreen
        setContent {
            TogetherWeCanWithGeminiTheme {
                MainScreen(
                    onLogoutClick = { signOut() },
                    viewModel = mainViewModel,
                    challengesViewModel = challengesViewModel
                )
            }
        }
    }

    private fun signOut() {
        // Sign out the user and navigate to AuthActivity
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()
            }
    }

    private fun getDeviceLocale(context: Context): Locale {
        // Retrieve the device's current locale
        return context.resources.configuration.locales[0]
    }
}

@Composable
fun MainScreen(
    onLogoutClick: () -> Unit,
    viewModel: MainViewModel,
    challengesViewModel: ChallengesViewModel
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val goalsState = viewModel.goals.collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            // Conditionally display the bottom navigation bar
            if (shouldShowBottomBar(navController)) {
                BottomNavigationBar(navController)
            }
        },
        content = { paddingValues ->
            // Set up navigation host with various composable destinations
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    HomeContent(
                        modifier = Modifier.padding(paddingValues),
                        goals = goalsState.value,
                        context = context,
                        viewModel = viewModel
                    )
                }
                composable(Screen.Discover.route) {
                    DiscoverScreen(viewModel = viewModel)
                }
                composable(Screen.Challenges.route) {
                    ChallengesScreen(viewModel = challengesViewModel, navController = navController)
                }
                composable("challenge_detail/{challengeId}") { backStackEntry ->
                    val challengeId = backStackEntry.arguments?.getString("challengeId")
                    ChallengeDetailScreen(challengeId = challengeId ?: "", viewModel = challengesViewModel)
                }
                composable("account/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    AccountScreen(userId = userId, onLogoutClick, viewModel = viewModel)
                }
                composable(Screen.Learn.route) {
                    LearnScreen(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                composable("sdg_detail/{name}/{description}/{index}/{imageUrl}/{sdgUrl}") { backStackEntry ->
                    val nameJson = Uri.decode(backStackEntry.arguments?.getString("name") ?: "{}")
                    val descriptionJson = Uri.decode(backStackEntry.arguments?.getString("description") ?: "{}")
                    val gson = Gson()
                    val nameMap: Map<String, String> = gson.fromJson(nameJson, object : TypeToken<Map<String, String>>() {}.type)
                    val descriptionMap: Map<String, String> = gson.fromJson(descriptionJson, object : TypeToken<Map<String, String>>() {}.type)
                    val indexString = backStackEntry.arguments?.getString("index") ?: "0"
                    val index = try {
                        indexString.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }
                    val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                    val encodedSdgUrl = backStackEntry.arguments?.getString("sdgUrl") ?: ""
                    val sdgUrl = URLDecoder.decode(encodedSdgUrl, "UTF-8")
                    val sdg = SDG(
                        name = nameMap,
                        description = descriptionMap,
                        index = index,
                        imageName = imageUrl,
                        sdgUrl = sdgUrl
                    )
                    Log.d("line 208", sdg.toString())
                    SDGDetailScreen(sdg = sdg)
                }
            }
        }
    )
}

@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    goals: List<Goal>,
    context: Context,
    viewModel: MainViewModel
) {
    val summary by viewModel.summary.collectAsState() // Collect summary state from ViewModel
    val appDetails by viewModel.appDetails.collectAsState()
    val (showCarousel, setShowCarousel) = remember { mutableStateOf(false) }
    val (showSummaryDialog, setShowSummaryDialog) = remember { mutableStateOf(false) }

    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        // Summary section with a button to show the carousel and a summary card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Absolute.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(LocalConfiguration.current.screenHeightDp.dp / 4)
            ) {
                Button(onClick = { setShowCarousel(true) },
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(painter = painterResource(id = R.drawable.logo),
                        contentDescription = stringResource(R.string.app_name),
                        tint = Color.Unspecified
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
                    contentDescription = "Click to learn more about app",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            summary?.let {
                SummaryCard(
                    summary = it,
                    onClick = { setShowSummaryDialog(true) }
                )
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LocalConfiguration.current.screenHeightDp.dp / 4)
                        .background(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = stringResource(R.string.fetching_daily_content),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
        // Show carousel dialog if needed
        if (showCarousel) {
            Dialog(properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { setShowCarousel(false) }) {
                Carousel(
                    details = appDetails,
                    localLanguageCode = viewModel.getLocaleLanguageCode(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                )
            }
        }
        // Goals section with a message if no goals exist
        if (goals.isEmpty()) {
            Text(stringResource(R.string.go_to_learn_section_and_know_more))
        } else {
            Box(modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color = MaterialTheme.colorScheme.primary)
                .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.goals),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(4.dp)
                        .background(color = MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(goals) { goal ->
                    GoalItem(goal = goal, onClick = {
                        val intent = Intent(context, GoalDetailActivity::class.java).apply {
                            putExtra("title", goal.title)
                            putExtra("description", goal.description)
                            putExtra("sdg", goal.sdg)
                            putExtra("goalId", goal.goalId)
                        }
                        context.startActivity(intent)
                    })
                }
            }
        }
    }

    // Show full-screen dialog for summary
    if (showSummaryDialog) {
        FullScreenSummaryDialog(
            summary = summary,
            onDismiss = { setShowSummaryDialog(false) }
        )
    }
}

@Composable
fun SummaryCard(summary: SummaryWithLanguage, onClick: () -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier
            .heightIn(max = screenHeight / 4)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(text = summary.title ?: "", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
                contentDescription = stringResource(R.string.tap_to_view_details),
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Composable
fun FullScreenSummaryDialog(
    summary: SummaryWithLanguage?,
    onDismiss: () -> Unit
) {
    requireNotNull(summary) // Ensure the dialog is only shown when summary is not null

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        // Use a Box to contain the scrollable Column
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(4.dp)
                .systemBarsPadding()
        ) {
            // Scrollable Column for summary details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Display HTML content or any other summary details
                val htmlContent = markdownToHtml(summary.summary ?: "")
                HtmlTextView(htmlContent)

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
fun GoalItem(goal: Goal, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = goal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Add the arrow icon to indicate clickability
        Icon(
            painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
            contentDescription = stringResource(R.string.click_to_view_details),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun shouldShowBottomBar(navController: NavController): Boolean {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val route = currentDestination?.route

    // Define routes where the bottom bar should be shown
    val routesWithBottomBar = setOf(
        Screen.Home.route,
        Screen.Discover.route,
        Screen.Settings.route,
        Screen.Challenges.route,
        Screen.Account.route,
        Screen.Learn.route
    )

    // Check if the bottom bar should be shown based on the current route
    return when {
        route in routesWithBottomBar -> true
        route?.startsWith("${Screen.Account.route}/") == true -> true
        else -> false
    }
}
