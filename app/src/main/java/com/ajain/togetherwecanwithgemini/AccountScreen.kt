import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.ajain.togetherwecanwithgemini.ActivityItem
import com.ajain.togetherwecanwithgemini.R
import com.ajain.togetherwecanwithgemini.data.Activity
import com.ajain.togetherwecanwithgemini.viewmodels.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountScreen(userId: String, onLogoutClick: () -> Unit, viewModel: MainViewModel = viewModel()) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val activities = remember { mutableStateListOf<Activity>() }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    var displayName by remember { mutableStateOf("") }
    var challengeCount by remember { mutableStateOf(0) }
    var activityCount by remember { mutableStateOf(0) }
    val currentUserId = auth.currentUser?.uid
    val isCurrentUser = userId == currentUserId

    Log.d("line 90", isCurrentUser.toString())
    LaunchedEffect(userId) {
        // Cancel existing listener if any
        listenerRegistration?.remove()

        val userRef = firestore.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document != null) {
                displayName = document.getString("displayName") ?: ""
                if (document.contains("activities")) {
                    val activityIds = document.get("activities") as List<*>
                    activityCount = activityIds.size
                    var query = firestore.collection("activities")
                        .whereIn(FieldPath.documentId(), activityIds.map { it.toString() })

                    if (!isCurrentUser) {
                        Log.d("AccountScreen", "Applying isPublished filter")
                        query = query.whereEqualTo("isPublished", true) // Create a new query with the filter
                        Log.d("line 104", query.toString())
                    } else {
                        Log.d("AccountScreen", "No filter applied for current user")
                    }

                    query.addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("AccountScreen", "Error fetching activities", e)
                            return@addSnapshotListener
                        }
                        snapshot?.let { querySnapshot ->
                            val updatedActivities = querySnapshot.documents.mapNotNull { doc ->
                                val data = doc.data!!
                                Activity(
                                    goalTitle = data["goalTitle"] as? String ?: "",
                                    detail = data["detail"] as? String ?: "",
                                    imageUrl = data["imageUrl"] as? String ?: "",
                                    timestamp = data["timestamp"] as? Long ?: 0L,
                                    userId = data["userId"] as? String ?: "",
                                    userDisplayName = data["userDisplayName"] as? String ?: "",
                                    id = doc.id,
                                    isPublished = data["isPublished"] as? Boolean ?: false,
                                    likeCount = data["likeCount"] as? Long ?: 0L
                                )
                            }
                            activities.clear()
                            activities.addAll(updatedActivities)
                           // activityCount = activities.size
                        }
                    }
                }
            }

            firestore.collection("challenge_entries")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("AccountScreen", "Error fetching challenge count", e)
                        return@addSnapshotListener
                    }
                    challengeCount = snapshot?.size() ?: 0
                }
        }
    }


    DisposableEffect(userId) {
        onDispose {
            listenerRegistration?.remove()
        }
    }

    fun shareDetails() {
        val shareText = """
            User: $displayName
            Till date, I have participated in $challengeCount challenges and $activityCount activities.
            Join me today on "Together We Can With Gemini" app.
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_via)))
    }

    fun deleteActivity(activityId: String) {
        val userRef = firestore.collection("users").document(userId)
        userRef.update("activities", FieldValue.arrayRemove(activityId))
            .addOnSuccessListener {
                firestore.collection("activities").document(activityId).delete()
                    .addOnSuccessListener {
                        activities.removeIf { it.id == activityId }
                    }
                    .addOnFailureListener { e ->
                        Log.e("AccountScreen", "Error deleting activity", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("AccountScreen", "Error updating user activities", e)
            }
    }

    fun publishActivity(activityId: String, isPublished: Boolean) {
        firestore.collection("activities").document(activityId).update("isPublished", !isPublished)
            .addOnSuccessListener {
                val updatedActivity = activities.find { it.id == activityId }
                updatedActivity?.isPublished = !isPublished
                activities.replaceAll { if (it.id == activityId) updatedActivity!! else it }
            }
            .addOnFailureListener { e ->
                Log.e("AccountScreen", "Error updating activity publication status", e)
            }
    }

    fun shareActivity(activity: Activity) {
        val storage = FirebaseStorage.getInstance()

        val encodedPath = activity.imageUrl.substringAfter("o/").substringBefore("?alt=media")
        val storagePath = URLDecoder.decode(encodedPath, "UTF-8")

        val storageRef = storage.reference.child(storagePath)

        try {
            val localFile = File.createTempFile("shared_image", ".jpg", context.cacheDir)

            storageRef.getFile(localFile).addOnSuccessListener {
                val fileUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", localFile)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "${activity.goalTitle}\n${activity.detail}")
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = "image/jpeg"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent,
                    context.getString(R.string.share_via)))
            }.addOnFailureListener { e ->
                Log.e("AccountScreen", "Error downloading image: ${e.message}", e)
            }
        } catch (e: IOException) {
            Log.e("AccountScreen", "Error creating temporary file: ${e.message}", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .systemBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Row containing display name, counts, and buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column containing display name
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1.1f), // Allows the column to take up only as much space as needed
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Row containing challenge and activity counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(2f) // Adjusts the width to distribute space between counts and buttons
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = MaterialTheme.colorScheme.primary)
                            .padding(4.dp)


                    ) {
                        Text(
                            text = "$challengeCount",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(text = "Challenges",
                            style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = MaterialTheme.colorScheme.primary)
                            .padding(4.dp)) {
                        Text(
                            text = "$activityCount",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(text = "Actions", style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // Row containing share and logout buttons (if applicable)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // Allocates space for buttons
                ) {
                    IconButton(onClick = { shareDetails() }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                    }

                    if (isCurrentUser) {
                        IconButton(onClick = onLogoutClick) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.logout))
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .let { if (isCurrentUser) it.padding(bottom = 80.dp) else it }
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                items(activities) { activity ->
                    ActivityItem(activity = activity, viewModel = viewModel, showActions = false)
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        if (isCurrentUser) {
                            IconButton(onClick = { shareActivity(activity) }) {
                                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                            }
                            IconButton(onClick = { publishActivity(activity.id, activity.isPublished) }) {
                                Icon(
                                    imageVector = if (activity.isPublished) Icons.Filled.AccountCircle else Icons.Filled.Lock,
                                    contentDescription = stringResource(R.string.publish)
                                )
                            }
                            IconButton(onClick = { deleteActivity(activity.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                            }
                            Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp) // Space between like count and icon
                        ) {
                            Text(text = "${activity.likeCount}", style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { }, enabled = false) {
                                Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.like))
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
