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

    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            // Call function to generate and save the unique display name directly
            LaunchedEffect(Unit) {
                handleUserDetails()
            }
        }
    }

    private fun handleUserDetails() {
        fetchAdjectivesAndAnimals { adjectives, animals ->
            generateUniqueDisplayName(adjectives, animals) { uniqueName ->
                saveUserDetails(uniqueName)
            }
        }
    }

    private fun fetchAdjectivesAndAnimals(callback: (List<String>, List<String>) -> Unit) {
        val adjectivesRef = db.collection("adjectives")
        val animalsRef = db.collection("animals")

        val adjectivesTask = adjectivesRef.get()
        val animalsTask = animalsRef.get()

        Tasks.whenAllSuccess<List<DocumentSnapshot>>(adjectivesTask, animalsTask)
            .addOnSuccessListener { results ->
                val adjectives = results[0].map { it.getString("name")!! }
                val animals = results[1].map { it.getString("name")!! }
                callback(adjectives, animals)
            }
            .addOnFailureListener {
                // Handle the error
            }
    }

    private fun generateUniqueDisplayName(adjectives: List<String>, animals: List<String>, callback: (String) -> Unit) {
        val usedNamesRef = db.collection("used_names")

        var uniqueName = ""
        do {
            val randomAdjective = adjectives.random()
            val randomAnimal = animals.random()
            uniqueName = "$randomAdjective $randomAnimal"
        } while (isNameInUse(usedNamesRef, uniqueName))

        callback(uniqueName)
    }

    private fun isNameInUse(usedNamesRef: CollectionReference, name: String): Boolean {
        val query = usedNamesRef.whereEqualTo("name", name).get()
        return query.isComplete && !query.result.isEmpty
    }

    private fun saveUserDetails(displayName: String) {
        val user = mAuth.currentUser
        if (user != null) {
            val userInfo = hashMapOf(
                "displayName" to displayName
            )

            db.collection("users").document(user.uid)
                .set(userInfo, SetOptions.merge())
                .addOnSuccessListener {
                    // Also save the name to the used_names collection to prevent future use
                    db.collection("used_names").document(user.uid).set(hashMapOf("name" to displayName))
                    launchMainActivity()
                }
                .addOnFailureListener {
                    Toast.makeText(this,
                        getString(R.string.error_saving_user_details), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
