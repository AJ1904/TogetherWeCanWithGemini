package com.ajain.togetherwecanwithgemini

import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.ajain.togetherwecanwithgemini.data.Challenge
import com.ajain.togetherwecanwithgemini.ui.theme.Blue64
import com.ajain.togetherwecanwithgemini.ui.theme.PurpleGrey40
import com.ajain.togetherwecanwithgemini.utils.loadSDGs
import com.ajain.togetherwecanwithgemini.viewmodels.ChallengesViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChallengeDetailScreen(challengeId: String, viewModel: ChallengesViewModel) {
    val challenges = viewModel.challenges.collectAsState().value
    val challenge = challenges.find { it.id == challengeId } ?: return
    var showDialog by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }

    var hasSubmitted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val userId = mAuth.currentUser?.uid

    val localLanguage = viewModel.getLocaleLanguage()
    val localLanguageCode = viewModel.getLocaleLanguageCode()

    // Load SDG data
    val sdgs = remember { loadSDGs(context) }
    val sdgNumber = challenge.sdg.substringAfter("SDG ").substringBefore(":").toIntOrNull()
    val sdg = sdgNumber?.let { number -> sdgs.find { it.index == number } }
    val sdgTranslation = sdg?.name?.get(localLanguageCode) ?: sdg?.name?.get("en")

    LaunchedEffect(challengeId, userId) {
        if (userId != null) {
            db.collection("challenge_entries")
                .whereEqualTo("challenge_id", challengeId)
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener { documents ->
                    hasSubmitted = !documents.isEmpty
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error checking submission status", e)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 64.dp) // To prevent overlap with BottomNavigationBar
                .systemBarsPadding()
        ) {
            // Title
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
// SDG Translation and Dates Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
             //   horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // SDG Translation
                sdgTranslation?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp)
                            // .padding(bottom = 8.dp)
                            .fillMaxWidth()
                    )
                }


            }
            // Dates
            Row (horizontalArrangement = Arrangement.Absolute.SpaceEvenly ,
                modifier = Modifier.fillMaxWidth()){
                DateInfo(
                    label = stringResource(R.string.start_date),
                    date = challenge.startDate
                )
                Spacer(modifier = Modifier.width(8.dp))
                DateInfo(
                    label = stringResource(R.string.end_date),
                    date = challenge.endDate
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { showLeaderboardDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor= MaterialTheme.colorScheme.tertiary,
                        contentColor= MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor= MaterialTheme.colorScheme.primary,
                        disabledContentColor= MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)

                ) {
                    Text(stringResource(R.string.leaderboard), style = MaterialTheme.typography.bodySmall)
                }
            }
Spacer(modifier = Modifier.height(8.dp))
            // Description
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

// Buttons
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showDialog = true },
                    enabled = !hasSubmitted,
                    colors = ButtonDefaults.buttonColors(
                        containerColor= MaterialTheme.colorScheme.tertiary,
                        contentColor= MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor= MaterialTheme.colorScheme.primary,
                        disabledContentColor= MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.submit_entry), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = { showEntryDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor= MaterialTheme.colorScheme.tertiary,
                        contentColor= MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor= MaterialTheme.colorScheme.primary,
                        disabledContentColor= MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)

                ) {
                    Text(stringResource(R.string.view_my_entry), style = MaterialTheme.typography.bodySmall)
                }

            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            // Evaluation Criteria
            Text(
                text = stringResource(R.string.evaluation_criteria),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            challenge.evaluationCriteria.forEachIndexed { index, criteria ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${criteria.criteria}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "${criteria.maxPoints}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

        }

        // Dialogs
        if (showDialog) {
            ChallengeEntryDialog(
                challengeId = challengeId,
                onDismiss = { showDialog = false },
                localLanguage = localLanguage,
                localLanguageCode = localLanguageCode,

                onSubmissionComplete = { success ->
                    if (success) {
                        Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                        hasSubmitted = true
                    } else {
                        Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                    }
                    showDialog = false
                    isSubmitting = false
                },
                isSubmitting = isSubmitting,
                setSubmitting = { isSubmitting = it }
            )
        }
        if (showEntryDialog) {
            ViewMyEntryDialog(
                challengeId = challengeId,
                onDismiss = { showEntryDialog = false },
                localLanguageCode = localLanguageCode
            )
        }
        if (showLeaderboardDialog) {
            ViewLeaderboardDialog(
                challengeId = challengeId,
                onDismiss = { showLeaderboardDialog = false },
                userId = userId?: ""
            )
        }
    }
}


@Composable
fun ViewLeaderboardDialog(challengeId: String, onDismiss: () -> Unit, userId: String = "") {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var userRank by remember { mutableStateOf<LeaderboardEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var noEntries by remember { mutableStateOf(false) }

    LaunchedEffect(challengeId) {
        db.collection("challenge_entries")
            .whereEqualTo("challenge_id", challengeId)
            .orderBy("totalScore", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val entries = documents.documents.mapNotNull { document ->
                    val userId = document.getString("user_id") ?: return@mapNotNull null
                    val totalScore = document.getLong("totalScore") ?: return@mapNotNull null
                    LeaderboardEntry(userId, totalScore)
                }

                if (entries.isEmpty()) {
                    noEntries = true
                    loading = false
                } else {
                    // Fetch display names for each user
                    val fetchDisplayNames = entries.map { entry ->
                        db.collection("users").document(entry.userId).get()
                            .continueWith { task ->
                                val displayName = task.result?.getString("displayName") ?: "Unknown"
                                entry.copy(displayName = displayName)
                            }
                    }
                    Tasks.whenAllSuccess<LeaderboardEntry>(fetchDisplayNames).addOnSuccessListener { updatedEntries ->
                        leaderboard = updatedEntries
                        if (userId.isNotEmpty()) {
                            db.collection("challenge_entries")
                                .whereEqualTo("challenge_id", challengeId)
                                .whereEqualTo("user_id", userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    if (!userDoc.isEmpty) {
                                        val userEntry = userDoc.documents[0]
                                        val userTotalScore = userEntry.getLong("totalScore") ?: 0
                                        val userEntryObject = LeaderboardEntry(userId, userTotalScore)
                                        userRank = if (updatedEntries.any { it.userId == userId }) {
                                            updatedEntries.find { it.userId == userId }
                                        } else {
                                            db.collection("users").document(userId).get()
                                                .continueWith { task ->
                                                    val displayName = task.result?.getString("displayName") ?: "Unknown"
                                                    userEntryObject.copy(displayName = displayName)
                                                }.result
                                        }
                                    }
                                    loading = false
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error fetching user entry", e)
                                    loading = false
                                }
                        } else {
                            loading = false
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching leaderboard", e)
                loading = false
            }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .background(MaterialTheme.colorScheme.background)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))) {
                        Text(
                            text = stringResource(id = R.string.leaderboard),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.close)
                            )
                        }
                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (loading) {
                        LoadingIndicator()
                    } else {
                        if (noEntries) {
                            Text(
                                text = stringResource(R.string.awaiting_participation_join_now),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background),
                                // .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.rank),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.name),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.scores),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            leaderboard.forEachIndexed { index, entry ->
                                LeaderboardEntryView(entry, index + 1, userId)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            userRank?.let {
                                if (leaderboard.none { it.userId == userId }) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    LeaderboardEntryView(it, leaderboard.size + 1, userId)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardEntryView(entry: LeaderboardEntry, rank: Int, currentUserId: String) {
    val backgroundColor = if (entry.userId == currentUserId) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha=0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$rank", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = entry.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = entry.totalScore.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
fun DateInfo(label: String, date: String) {
    Column  {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
@Composable
fun ChallengeEntryDialog(challengeId: String, onDismiss: () -> Unit, localLanguage: String, localLanguageCode: String,onSubmissionComplete: (Boolean) -> Unit,
                         isSubmitting: Boolean,
                         setSubmitting: (Boolean) -> Unit) {
    var description by remember { mutableStateOf("") }
    var mediaUris by remember { mutableStateOf(listOf<Uri>()) }
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("CameraLauncher", "Camera result received")
        if (result.resultCode == RESULT_OK) {
            photoUri?.let {
                Log.d("CameraLauncher", "Image URI: $it")
                mediaUris = mediaUris + it
                photoUri = null
            }
        } else {
            Log.e("CameraLauncher", "Camera result not OK: ${result.resultCode}")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("PermissionLauncher", "Camera permission granted: $isGranted")
        if (isGranted) {
            photoUri = createImageFileUri(context)
            dispatchTakePictureIntent(context, cameraLauncher, photoUri)
        } else {
            // Show alert that permission is required
            Log.e("CameraPermission", "Camera permission is required")
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
        ){
        Box(modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(MaterialTheme.colorScheme.background)
        ) {
            if (isSubmitting) {
                LoadingIndicator()
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.close)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.submit_your_entry), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        .padding(8.dp)
                        .heightIn(min = 100.dp) // Minimum height to ensure multiple lines are visible


                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.media_files))
                Spacer(modifier = Modifier.height(8.dp))
                ImageGrid(mediaUris, context, cameraLauncher, permissionLauncher, onImageCapture = { uri ->
                    photoUri = uri
                })
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        setSubmitting(true)
                        handleSubmission(context, challengeId, description, mediaUris, onDismiss, localLanguage, localLanguageCode,
                                onSuccess = {
//                            isLoading = false
//                            showToast("Success")
//                            setHasSubmitted(true)
                                    setSubmitting(false)
                                    onSubmissionComplete(true)
                            onDismiss()
                        },
                            onError = {
//                                isLoading = false
//                                showToast("Error")
                                setSubmitting(false)
                                onSubmissionComplete(false)
                            }//,
                            //setHasSubmitted = { hasSubmitted = it }
                        )

                              },
                    enabled = description.isNotEmpty() && mediaUris.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor= MaterialTheme.colorScheme.tertiary,
                        contentColor= MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor= PurpleGrey40,
                        disabledContentColor= MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.submit))
                }

            }
        }
            }
    }
}

@Composable
fun ImageGrid(
    mediaUris: List<Uri>,
    context: Context,
    cameraLauncher: ActivityResultLauncher<Intent>,
    permissionLauncher: ActivityResultLauncher<String>,
    onImageCapture: (Uri?) -> Unit
) {
    val maxImages = 4
    val gridItems = (mediaUris + List(maxImages - mediaUris.size) { null }).take(maxImages)

    Column {
        for (row in gridItems.chunked(2)) {
            Row {
                row.forEach { uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                            Log.d("ImageGrid", "Displaying image URI: $uri")
                        } else {
                            IconButton(onClick = {
                                if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    val newPhotoUri = createImageFileUri(context)
                                    onImageCapture(newPhotoUri)
                                    dispatchTakePictureIntent(context, cameraLauncher, newPhotoUri)
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = stringResource(R.string.add_photo),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun handleSubmission(
    context: Context,
    challengeId: String,
    description: String,
    mediaUris: List<Uri>,
    onDismiss: () -> Unit,
    localLanguage: String,
    localLanguageCode: String,
    onSuccess: () -> Unit,
    onError: () -> Unit,

) {
    val storage = FirebaseStorage.getInstance()
    val db = FirebaseFirestore.getInstance()
    val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val userId = mAuth.currentUser?.uid

    val uploadTasks = mediaUris.map { uri ->
        val storageRef = storage.reference.child("challenges/$challengeId/$userId/${uri.lastPathSegment}")
        storageRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }
    }

    Tasks.whenAllSuccess<Uri>(uploadTasks).addOnSuccessListener { downloadUrls ->
        val entryData = hashMapOf(
            "challenge_id" to challengeId,
            "user_id" to userId,
            "entry_description" to description,
            "photo_urls" to downloadUrls.map { it.toString() },
            "localLanguage" to localLanguage,
            "localLanguageCode" to localLanguageCode
        )

        db.collection("challenge_entries")
            .add(entryData)
            .addOnSuccessListener {
                Log.d("Firestore", "Entry added successfully")

                onSuccess()
                onDismiss()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding entry", e)
                onError()
            }
    }.addOnFailureListener { e ->
        Log.e("FirebaseStorage", "Error uploading images", e)
        onError()
    }
}


@Composable
fun ViewMyEntryDialog(challengeId: String, onDismiss: () -> Unit, localLanguageCode: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val userId = mAuth.currentUser?.uid

    var description by remember { mutableStateOf<String?>(null) }
    var photoUrls by remember { mutableStateOf<List<String>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var scores by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var summary by remember { mutableStateOf<String?>(null) }
    var criteriaMap by remember { mutableStateOf<Map<String, String>?>(null) }
    var criteriaEnglishToLocal by remember { mutableStateOf<Map<String, String>?>(null) }

    LaunchedEffect(Unit) {
        userId?.let {
            db.collection("challenge_entries")
                .whereEqualTo("challenge_id", challengeId)
                .whereEqualTo("user_id", it)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val entry = documents.documents[0]
                        description = entry.getString("entry_description")
                        photoUrls = entry.get("photo_urls") as? List<String>
                        scores = entry.get("scores") as? List<Map<String, Any>>
                        summary = entry.getString("summary")

                        db.collection("challenges")
                            .document(challengeId)
                            .get()
                            .addOnSuccessListener { challengeDoc ->
                                if (challengeDoc.exists()) {
                                    val challenge = challengeDoc.data
                                    val criteria = challenge?.get("evaluationCriteria") as? List<Map<String, Any>> ?: emptyList()
                                    val localCriteriaMap = criteria.associate { criteriaEntry ->
                                        val criteriaTextMap = criteriaEntry["criteria"] as? Map<String, String> ?: emptyMap()
                                        val criteriaText = criteriaTextMap[localLanguageCode] ?: criteriaTextMap["en"] ?: ""
                                        criteriaText to criteriaEntry["maxPoints"].toString()
                                    }
                                    val englishToLocalMap = criteria.associate { criteriaEntry ->
                                        val criteriaTextMap = criteriaEntry["criteria"] as? Map<String, String> ?: emptyMap()
                                        val englishText = criteriaTextMap["en"] ?: ""
                                        val localText = criteriaTextMap[localLanguageCode] ?: englishText
                                        englishText to localText
                                    }
                                    criteriaMap = localCriteriaMap
                                    criteriaEnglishToLocal = englishToLocalMap
                                }
                            }
                            .addOnFailureListener { e -> Log.e("Firestore", "Error fetching challenge", e) }
                    }
                    loading = false
                }
                .addOnFailureListener { e -> Log.e("Firestore", "Error fetching entry", e); loading = false }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .background(MaterialTheme.colorScheme.background)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))) {
                        Text(
                            text = stringResource(id = R.string.your_entry),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.your_entry)
                            )
                        }
                    }
                    HorizontalDivider()
                    if (loading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LoadingIndicator()
                    } else {
                        if (description != null && photoUrls != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = description ?: "",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            photoUrls?.let { urls ->
                                ImageGrid(urls.map { Uri.parse(it) }, context)
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (scores != null && summary != null) {
//
                                Text(
                                    text = stringResource(R.string.scores),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column {
                                    scores?.forEach { score ->
                                        val criteriaEnglish = score["criteria"] as? String ?: ""
                                        val criteriaText =
                                            criteriaEnglishToLocal?.get(criteriaEnglish)
                                                ?: criteriaEnglish
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.1f
                                                    )
                                                )
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = criteriaText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp) // Make this size square
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${score["score"]}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.summary),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = summary
                                            ?: stringResource(R.string.no_summary_available),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.wait_for_results),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = stringResource(R.string.no_entry_found_submit_your_entry_and_join_the_challenge_now),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageGrid(
    mediaUris: List<Uri>,
    context: Context,
    cameraLauncher: ActivityResultLauncher<Intent>? = null,
    permissionLauncher: ActivityResultLauncher<String>? = null,
    onImageCapture: (Uri?) -> Unit = {}
) {
    val maxImages = 4
    val gridItems = mediaUris.take(maxImages) // Take only the available images, up to the maximum limit

    Column {
        for (row in gridItems.chunked(2)) {
            Row {
                row.forEach { uri ->
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Display add photo button if there is space for more images
        if (mediaUris.size < maxImages && cameraLauncher != null && permissionLauncher != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val newPhotoUri = createImageFileUri(context)
                            onImageCapture(newPhotoUri)
                            dispatchTakePictureIntent(context, cameraLauncher, newPhotoUri)
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add_photo),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


private fun dispatchTakePictureIntent(context: Context, cameraLauncher: ActivityResultLauncher<Intent>, uri: Uri?) {
    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
    try {
        cameraLauncher.launch(takePictureIntent)
        Log.d("DispatchIntent", "Camera intent launched")
    } catch (e: ActivityNotFoundException) {
        // display error state to the user
        Log.e("DispatchIntent", "Camera app not found", e)
    }
}

private fun createImageFileUri(context: Context): Uri? {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.externalCacheDir // Use the external cache directory
    val imageFile = File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

data class LeaderboardEntry(
    val userId: String,
    val totalScore: Long,
    val displayName: String = ""
)
