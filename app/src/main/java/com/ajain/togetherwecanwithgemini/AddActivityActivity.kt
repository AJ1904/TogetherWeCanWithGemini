package com.ajain.togetherwecanwithgemini

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.ajain.togetherwecanwithgemini.ui.theme.TogetherWeCanWithGeminiTheme
import com.ajain.togetherwecanwithgemini.viewmodels.ActivityViewModel
import com.ajain.togetherwecanwithgemini.UiState
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*
class AddActivityActivity : ComponentActivity() {

    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val activityViewModel: ActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Retrieve goal title and user ID from intent extras
        val goalTitle = intent.getStringExtra("goalTitle") ?: ""
        val userId = intent.getStringExtra("userId") ?: ""

        // Initialize Firebase App Check for security
        val providerFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(providerFactory)

        // Fetch user display name from Firestore and set content view
        fetchUserDisplayName(userId) { userDisplayName ->
            setContent {
                TogetherWeCanWithGeminiTheme {
                    AddActivityScreen(goalTitle, userId, userDisplayName, activityViewModel)
                }
            }
        }
    }

    // Fetches the user's display name from Firestore
    private fun fetchUserDisplayName(userId: String, callback: (String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val displayName = document.getString("displayName") ?: "Unknown"
                    callback(displayName)
                } else {
                    callback("Unknown")
                }
            }
            .addOnFailureListener { e ->
                Log.w("AddActivityActivity", "Error fetching user displayName", e)
                callback("Unknown")
            }
    }
}

@Composable
fun AddActivityScreen(goalTitle: String, userId: String, userDisplayName: String, activityViewModel: ActivityViewModel = viewModel()) {
    // State variables for managing input and UI state
    var detail by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var generatedContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    // Launcher for selecting image from gallery
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    // Observe UI state from ViewModel
    val uiState by activityViewModel.uiState.collectAsState()

    // Composable UI layout
    LazyColumn(modifier = Modifier
        .padding(16.dp)
        .systemBarsPadding()) {
        
        // Input field for activity details
        item {
            BasicTextField(
                value = detail,
                onValueChange = { detail = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (detail.isEmpty()) {
                            Text(
                                text = stringResource(R.string.enter_details_here),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Button to upload an image and display it
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { pickImage.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(stringResource(R.string.upload_photo))
                }

                imageUri?.let {
                    Spacer(modifier = Modifier.width(16.dp))
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(it)
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        // Button to generate and display content using Geminify
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    imageUri?.let { uri ->
                        coroutineScope.launch {
                            val bitmap = loadImageBitmap(context, uri)
                            bitmap?.let {
                                activityViewModel.geminifyContent(detail, it)
                            }
                        }
                    }
                },
                enabled = detail.isNotEmpty() && imageUri != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.geminify))
            }
        }

        // Display generated content or errors
        item {
            Spacer(modifier = Modifier.height(16.dp))
            when (uiState) {
                is UiState.Loading -> LoadingIndicator()
                is UiState.Success -> {
                    generatedContent = (uiState as UiState.Success).outputText
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = generatedContent,
                            onValueChange = { generatedContent = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .padding(bottom = 16.dp),
                            decorationBox = { innerTextField ->
                                if (generatedContent.isEmpty()) {
                                    Text(text = stringResource(R.string.generated_content_will_appear_here), color = Color.Gray)
                                }
                                innerTextField()
                            }
                        )
                        Button(onClick = { detail = generatedContent }, shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )) {
                            Text(stringResource(R.string.select))
                        }
                    }
                }
                is UiState.Error -> {
                    Text(text = stringResource(
                        R.string.error,
                        (uiState as UiState.Error).errorMessage
                    ))
                }
                is UiState.Initial -> {}
            }
        }

        // Final button to add the activity
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                enabled = detail.isNotEmpty() && imageUri != null,
                onClick = {
                    if (detail.isNotEmpty() && imageUri != null) {
                        isLoading = true // Show loading indicator
                        val imageRef = storage.reference.child("images/${UUID.randomUUID()}")
                        coroutineScope.launch {
                            val bitmap = loadImageBitmap(context, imageUri!!)
                            bitmap?.let {
                                uploadCompressedImage(
                                    it,
                                    imageRef,
                                    {
                                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                                            val activity = hashMapOf(
                                                "detail" to detail,
                                                "imageUrl" to uri.toString(),
                                                "goalTitle" to goalTitle,
                                                "timestamp" to System.currentTimeMillis(),
                                                "userId" to userId,
                                                "userDisplayName" to userDisplayName
                                            )
                                            firestore.collection("activities")
                                                .add(activity)
                                                .addOnSuccessListener { docRef ->
                                                    val activityId = docRef.id
                                                    val userRef = firestore.collection("users").document(userId)
                                                    userRef.update("activities", FieldValue.arrayUnion(activityId))
                                                        .addOnSuccessListener {
                                                            isLoading = false // Hide loading indicator
                                                            Toast.makeText(
                                                                context,
                                                                context.getString(R.string.success),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            (context as? Activity)?.finish() // Return to previous screen
                                                        }
                                                        .addOnFailureListener {
                                                            isLoading = false // Hide loading indicator
                                                            Toast.makeText(
                                                                context,
                                                                context.getString(R.string.failure),
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }
                                                .addOnFailureListener {
                                                    isLoading = false // Hide loading indicator
                                                }
                                        }.addOnFailureListener {
                                            isLoading = false // Hide loading indicator
                                        }
                                    },
                                    { exception ->
                                        Log.e("AddActivityScreen", "Upload failed", exception)
                                        isLoading = false // Hide loading indicator
                                    }
                                )
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_activity))
            }
        }
    }

    // Show loading indicator if data is being processed
    if (isLoading) {
        LoadingIndicator()
    }
}

// Loads a bitmap from the given URI
suspend fun loadImageBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return withContext(Dispatchers.IO) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .build()

        val result = (loader.execute(request) as? SuccessResult)?.drawable
        (result as? BitmapDrawable)?.bitmap
    }
}

// Compresses the bitmap and returns it as a byte array
fun compressBitmap(bitmap: Bitmap, quality: Int = 80): ByteArray {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}

// Uploads the compressed bitmap to Firebase Storage
fun uploadCompressedImage(bitmap: Bitmap, storageReference: StorageReference, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
    val compressedImage = compressBitmap(bitmap)
    val uploadTask = storageReference.putBytes(compressedImage)

    uploadTask.addOnSuccessListener {
        storageReference.downloadUrl.addOnSuccessListener { uri ->
            // Handle success, e.g., save URL to Firestore
            onSuccess()
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }.addOnFailureListener { exception ->
        onFailure(exception)
    }
}
