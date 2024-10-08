package com.ajain.togetherwecanwithgemini.goals

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.lang.UCharacter.toLowerCase
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ajain.togetherwecanwithgemini.AddActivityActivity
import com.ajain.togetherwecanwithgemini.PlaceSearchActivity
import com.ajain.togetherwecanwithgemini.data.Step
import com.ajain.togetherwecanwithgemini.ui.theme.TogetherWeCanWithGeminiTheme
import com.ajain.togetherwecanwithgemini.viewmodels.SDGViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.ajain.togetherwecanwithgemini.R
import com.ajain.togetherwecanwithgemini.utils.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

// Activity to handle detailed view and management of a specific goal
class GoalDetailActivity : ComponentActivity() {

    // Firebase Authentication instance for user management
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    // ViewModel instance for managing SDG-related data
    private val sdgViewModel: SDGViewModel by viewModels()

    // FusedLocationProviderClient for accessing location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge() // Enable edge-to-edge display

        // Retrieve goal details from intent extras
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val sdg = intent.getStringExtra("sdg") ?: ""
        val user = mAuth.currentUser
        val goalId = intent.getStringExtra("goalId") ?: ""

        // Fetch steps related to the goal
        if (user != null) {
            fetchSteps(user.uid, title)
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the UI with Compose
        setContent {
            TogetherWeCanWithGeminiTheme {
                if (user != null) {
                    // Observe ViewModel state for steps
                    val suggestedSteps by sdgViewModel.steps.collectAsState()
                    val firestoreSteps by sdgViewModel.firestoreSteps.collectAsState()

                    // Render the goal detail screen
                    GoalDetailScreen(
                        title = title,
                        description = description,
                        sdg = sdg,
                        onAddActivityClick = {
                            // Handle add activity button click
                            val intent = Intent(this, AddActivityActivity::class.java).apply {
                                putExtra("goalTitle", title)
                                putExtra("userId", user.uid)
                            }
                            startActivity(intent)
                        },
                        onSuggestStepsClick = {
                            // Handle suggest steps button click
                            if (!LocationUtils.hasLocationPermission(this)) {
                                LocationUtils.requestLocationPermission(this)
                            } else {
                                LocationUtils.getLocationAndGeocode(this, fusedLocationClient) { location ->
                                    Log.d("Location aj:", "Location: $location")
                                    sdgViewModel.suggestStepsForGoal(title, description, sdg, location ?: "")
                                }
                            }
                            sdgViewModel.suggestStepsForGoal(title, description, sdg, "")
                        },
                        firestoreSteps = firestoreSteps,
                        suggestedSteps = suggestedSteps,
                        onAddStepClick = { step ->
                            addStepToFirestore(user.uid, title, step)
                        },
                        onRemoveStepClick = { stepId ->
                            removeStepFromFirestore(user.uid, title, stepId)
                        },
                        onToggleStepCompletion = { stepId, isCompleted ->
                            updateStepCompletion(user.uid, title, stepId, isCompleted)
                        },
                        onSearchPlacesClick = { query ->
                            searchNearbyPlaces(query)
                        },
                        goalId = goalId
                    )
                }
            }
        }
    }

    // Fetches steps associated with the given goal from Firestore
    private fun fetchSteps(userId: String, goalTitle: String) {
        val db = FirebaseFirestore.getInstance()
        val stepsRef = db.collection("users").document(userId)
            .collection("goals").document(goalTitle).collection("steps")

        stepsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("Firestore", "Error fetching steps", e)
                return@addSnapshotListener
            }

            val steps = snapshot?.documents?.mapNotNull { doc ->
                val stepText = doc.getString("step")
                val query = doc.getString("query")
                if (stepText != null) {
                    Log.d("fetchSteps", "Fetched step: $stepText with query: $query")
                    Step(doc.id, stepText, doc.getBoolean("completed") ?: false, query ?: "")
                } else {
                    null
                }
            } ?: emptyList()

            sdgViewModel.setFirestoreSteps(steps)
        }
    }

    // Adds a new step to the Firestore database under the specified goal
    private fun addStepToFirestore(userId: String, goalTitle: String, step: String) {
        val db = FirebaseFirestore.getInstance()
        val goalRef = db.collection("users").document(userId).collection("goals").document(goalTitle)

        sdgViewModel.isThisStepRelatedToMaps(step) { query ->
            Log.d("addStepToFirestore", "Query generated: $query") // Log the generated query
            val stepData = mapOf(
                "step" to step,
                "completed" to false,
                "query" to query
            )

            goalRef.collection("steps").add(stepData)
                .addOnSuccessListener { Log.d("Firestore", "Step added successfully") }
                .addOnFailureListener { e -> Log.w("Firestore", "Error adding step", e) }
        }
    }

    // Removes a step from the Firestore database
    private fun removeStepFromFirestore(userId: String, goalTitle: String, stepId: String) {
        val db = FirebaseFirestore.getInstance()
        val stepRef = db.collection("users").document(userId).collection("goals").document(goalTitle)
            .collection("steps").document(stepId)

        stepRef.delete()
            .addOnSuccessListener { Log.d("Firestore", "Step removed successfully") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error removing step", e) }
    }

    // Updates the completion status of a step in the Firestore database
    private fun updateStepCompletion(userId: String, goalTitle: String, stepId: String, isCompleted: Boolean) {
        val db = FirebaseFirestore.getInstance()
        val stepRef = db.collection("users").document(userId).collection("goals").document(goalTitle)
            .collection("steps").document(stepId)

        stepRef.update("completed", isCompleted)
            .addOnSuccessListener { Log.d("Firestore", "Step completion updated successfully") }
            .addOnFailureListener { e -> Log.w("Firestore", "Error updating step completion", e) }
    }

    // Initiates a place search activity based on the provided query
    private fun searchNearbyPlaces(query: String) {
        // Create an intent to start a new activity that handles the place search
        val intent = Intent(this@GoalDetailActivity, PlaceSearchActivity::class.java).apply {
            putExtra("query", query)
        }
        startActivity(intent)
    }
}

// Composable function to display and manage goal details
@Composable
fun GoalDetailScreen(
    title: String,
    description: String,
    sdg: String,
    onAddActivityClick: () -> Unit,
    onSuggestStepsClick: () -> Unit,
    firestoreSteps: List<Step>?,
    suggestedSteps: List<Step>?,
    onAddStepClick: (String) -> Unit,
    onRemoveStepClick: (String) -> Unit,
    onToggleStepCompletion: (String, Boolean) -> Unit,
    onSearchPlacesClick: (String) -> Unit,
    goalId: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        item {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row (
                horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
                modifier = Modifier.fillMaxWidth())
            {
                Button(onClick = onAddActivityClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ), shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = stringResource(R.string.add_activity))
                }
                Button(onClick = onSuggestStepsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = stringResource(R.string.suggest_steps))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Display list of steps fetched from Firestore
        firestoreSteps?.let { stepList ->
            items(stepList) { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = step.completed,
                            onCheckedChange = { isChecked ->
                                onToggleStepCompletion(step.id, isChecked)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = step.stepText, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row {
                        Button(
                            colors = ButtonDefaults.buttonColors(Color.Red),
                            onClick = { onRemoveStepClick(step.id) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_step))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (toLowerCase(step.query) != "no") {
                            Button(
                                colors = ButtonDefaults.buttonColors(Color.Blue),
                                onClick = { onSearchPlacesClick(step.query) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = stringResource(R.string.search_places))
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        // Display list of suggested steps
        suggestedSteps?.let { stepList ->
            items(stepList) { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = step.stepText, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onAddStepClick(step.stepText) }, shape = RoundedCornerShape(8.dp)) {
                        Text(text = stringResource(R.string.add))
                    }
                }
            }
        }
    }
}
