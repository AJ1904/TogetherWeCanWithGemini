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


private val signInLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val response = IdpResponse.fromResultIntent(result.data)
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            if (response?.isNewUser == true) {
                launchUserDetailsActivity()
            } else {
                launchMainActivity()
            }
        } else {
            val errorMessage = response?.error?.localizedMessage ?: "Unknown error"
            showError(errorMessage)
        }
    }

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
                    // User is already signed in, launch the main activity
                    LaunchedEffect(Unit) {
                        launchMainActivity()
                    }
                } else {
                    // Show the custom UI with logo, name, and sign-in button
                    SignInScreen(sdgs = sdgs, onSignInClicked = {
                        startSignIn()
                    })
                }
            }
        }
    }

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

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


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
    val languages = textMap.keys.toList()
    var currentLanguageIndex by remember { mutableStateOf(0) }
    val currentText = textMap[languages[currentLanguageIndex]] ?: textMap["en"] ?: ""

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Change text every 2 seconds
            currentLanguageIndex = (currentLanguageIndex + 1) % languages.size
        }
    }

    Text(
        text = currentText,
        fontSize = 12.sp, // Smaller font size
       // style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray) // Adjust opacity with color
    )
}
@Composable
fun SignInScreen(
    sdgs: List<SDG>,
    onSignInClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
       //  Background: Animated SDGs
//        AnimatedSDGs(
//            sdgs = sdgs,
//            modifier = Modifier
//                .fillMaxSize()
//                //.fillMaxHeight(0.7f)
//                .padding(16.dp)
//                .graphicsLayer(alpha = 0.3f)
//        )

        Box( modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()){
            Image(
                painter = painterResource(id = R.drawable.bg_1),
                contentDescription = "App Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds

                //modifier = Modifier.fillMaxHeight().fillMaxWidth()

            )
        }
      //  VideoBackground()
        // Foreground: Logo, Text, and Button
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(16.dp)
//                .systemBarsPadding(),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Image(
//                painter = painterResource(id = R.drawable.logo),
//                contentDescription = "App Logo",
//                modifier = Modifier.
//                    fillMaxSize(0.6f)
//                //size(100.dp)
//            )
         //   Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = "TOGETHER WE CAN WITH GEMINI",
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onBackground,
//              //  fontStyle = MaterialTheme.typography.bodyLarge
//                //  color = Color.Black
//            )
//            Spacer(modifier = Modifier.height(32.dp))
//            Button(onClick = onSignInClicked,
//                colors = ButtonDefaults.buttonColors(
//                    containerColor= MaterialTheme.colorScheme.primary,
//                    contentColor= MaterialTheme.colorScheme.onPrimary,
//                ),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Text(text = "Sign in with Google", fontSize = 18.sp)
//            }
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
   // }
}

@Composable
fun AnimatedSDGs(sdgs: List<SDG>, modifier: Modifier = Modifier) {
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


