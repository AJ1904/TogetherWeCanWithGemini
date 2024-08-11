package com.ajain.togetherwecanwithgemini

import android.content.Intent
import android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.ajain.togetherwecanwithgemini.data.SDG
import com.ajain.togetherwecanwithgemini.ui.theme.PurpleGrey40
import com.ajain.togetherwecanwithgemini.ui.theme.TogetherWeCanWithGeminiTheme
import com.ajain.togetherwecanwithgemini.utils.loadSDGs
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay



class AuthActivity : ComponentActivity() {

    // Activity result launcher for handling sign-in responses
    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val response = IdpResponse.fromResultIntent(result.data)
            if (result.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (response?.isNewUser == true) {
                    // Launch User Details activity for new users
                    launchUserDetailsActivity()
                } else {
                    // Launch Main activity for existing users
                    launchMainActivity()
                }
            } else {
                // Display error message if sign-in fails
                val errorMessage = response?.error?.localizedMessage ?: "Unknown error"
                showError(errorMessage)
            }
        }

    // Display error message in ErrorDisplayActivity
    private fun showError(errorMessage: String) {
        val intent = Intent(this, ErrorDisplayActivity::class.java).apply {
            putExtra("error_message", errorMessage)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        val sdgs = loadSDGs(this) // Load SDGs from JSON

        setContent {
            TogetherWeCanWithGeminiTheme {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // Launch Main activity if user is already signed in
                    LaunchedEffect(Unit) {
                        launchMainActivity()
                    }
                } else {
                    // Display sign-in screen for unauthenticated users
                    SignInScreen(sdgs = sdgs, onSignInClicked = {
                        startSignIn()
                    })
                }
            }
        }
    }

    // Start the sign-in process
    private fun startSignIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.logo)
            .setTheme(R.style.Theme_TogetherWeCanWithGemini)
            .build()
        signInLauncher.launch(signInIntent)
    }

    // Launch the Main activity
    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Launch User Details activity for new users
    private fun launchUserDetailsActivity() {
        val user = FirebaseAuth.getInstance().currentUser
        val firstName = user?.displayName?.split(" ")?.getOrNull(0) ?: ""
        val lastName = user?.displayName?.split(" ")?.getOrNull(1) ?: ""
        val email = user?.email ?: ""

        val intent = Intent(this, UserDetailsActivity::class.java).apply {
            putExtra("firstName", firstName)
            putExtra("lastName", lastName)
            putExtra("email", email)
        }

        startActivity(intent)
        finish()
    }
}

@Composable
fun SmallAnimatedText(textMap: Map<String, String>) {
    // Manage and cycle through different texts for each language
    val languages = textMap.keys.toList()
    var currentLanguageIndex by remember { mutableStateOf(0) }
    val currentText = textMap[languages[currentLanguageIndex]] ?: textMap["en"] ?: ""

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Change text every 2 seconds
            currentLanguageIndex = (currentLanguageIndex + 1) % languages.size
        }
    }

    // Display the animated text with a smaller font size
    Text(
        text = currentText,
        fontSize = 12.sp, // Smaller font size
    )
}

@Composable
fun SignInScreen(
    sdgs: List<SDG>,
    onSignInClicked: () -> Unit
) {
    // Display the sign-in screen with background image and sign-in button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()) {
            Image(
                painter = painterResource(id = R.drawable.bg_1),
                contentDescription = "App Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)  // Padding to ensure it is not at the very edge
        ) {
            Button(
                onClick = onSignInClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Sign in with Google", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun AnimatedSDGs(sdgs: List<SDG>, modifier: Modifier = Modifier) {
    // Display each SDG with animated text
    Column(
        modifier = modifier
            .fillMaxHeight(), // Ensure Column takes up full height
        verticalArrangement = Arrangement.SpaceEvenly, // Adjust spacing between items
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // For each SDG, display an AnimatedText composable
        sdgs.forEachIndexed { _, sdg ->
            val textMap = sdg.name // This is the map of languages to SDG names
            SmallAnimatedText(textMap = textMap)
        }
    }
}

class ErrorDisplayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Retrieve and display error message
        val errorMessage = intent.getStringExtra("error_message") ?: "An error occurred"

        setContent {
            TogetherWeCanWithGeminiTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
