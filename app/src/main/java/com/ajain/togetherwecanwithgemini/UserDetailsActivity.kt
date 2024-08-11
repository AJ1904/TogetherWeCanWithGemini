package com.ajain.togetherwecanwithgemini

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot

class UserDetailsActivity : ComponentActivity() {

    // Lazily initialized Firebase Auth instance
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    
    // Lazily initialized Firestore instance
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure window settings for edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            // Call function to handle user details generation and saving
            LaunchedEffect(Unit) {
                handleUserDetails()
            }
        }
    }

    // Fetch adjectives and animals from Firestore and generate a unique display name
    private fun handleUserDetails() {
        fetchAdjectivesAndAnimals { adjectives, animals ->
            generateUniqueDisplayName(adjectives, animals) { uniqueName ->
                saveUserDetails(uniqueName)
            }
        }
    }

    // Fetch lists of adjectives and animals from Firestore
    private fun fetchAdjectivesAndAnimals(callback: (List<String>, List<String>) -> Unit) {
        val adjectivesRef = db.collection("adjectives")
        val animalsRef = db.collection("animals")

        // Retrieve adjectives and animals concurrently
        val adjectivesTask = adjectivesRef.get()
        val animalsTask = animalsRef.get()

        Tasks.whenAllSuccess<List<DocumentSnapshot>>(adjectivesTask, animalsTask)
            .addOnSuccessListener { results ->
                // Extract data and invoke callback
                val adjectives = results[0].map { it.getString("name")!! }
                val animals = results[1].map { it.getString("name")!! }
                callback(adjectives, animals)
            }
            .addOnFailureListener {
                // Handle errors during data retrieval
            }
    }

    // Generate a unique display name from random adjectives and animals
    private fun generateUniqueDisplayName(adjectives: List<String>, animals: List<String>, callback: (String) -> Unit) {
        val usedNamesRef = db.collection("used_names")

        var uniqueName = ""
        do {
            // Randomly combine adjective and animal
            val randomAdjective = adjectives.random()
            val randomAnimal = animals.random()
            uniqueName = "$randomAdjective $randomAnimal"
        } while (isNameInUse(usedNamesRef, uniqueName)) // Ensure name is unique

        callback(uniqueName)
    }

    // Check if the generated name is already used
    private fun isNameInUse(usedNamesRef: CollectionReference, name: String): Boolean {
        val query = usedNamesRef.whereEqualTo("name", name).get()
        return query.isComplete && !query.result.isEmpty
    }

    // Save the user details and update Firestore
    private fun saveUserDetails(displayName: String) {
        val user = mAuth.currentUser
        if (user != null) {
            val userInfo = hashMapOf(
                "displayName" to displayName
            )

            // Update user document and save the used name
            db.collection("users").document(user.uid)
                .set(userInfo, SetOptions.merge())
                .addOnSuccessListener {
                    // Also save the name to the used_names collection to prevent future use
                    db.collection("used_names").document(user.uid).set(hashMapOf("name" to displayName))
                    launchMainActivity() // Proceed to main activity
                }
                .addOnFailureListener {
                    // Show error message if saving fails
                    Toast.makeText(this,
                        getString(R.string.error_saving_user_details), Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Launch the main activity and finish current activity
    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
