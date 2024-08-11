package com.ajain.togetherwecanwithgemini.viewmodels

import androidx.lifecycle.ViewModel
import com.ajain.togetherwecanwithgemini.data.Challenge
import com.ajain.togetherwecanwithgemini.data.EvaluationCriteria
import com.ajain.togetherwecanwithgemini.data.Goal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ViewModel class responsible for managing and fetching challenge data from Firestore.
class ChallengesViewModel : BaseViewModel() {

    // FirebaseAuth instance to manage user authentication.
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    // FirebaseFirestore instance to interact with Firestore database.
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // StateFlow to hold and manage the list of challenges, enabling reactive UI updates.
    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges

    // Initialize the ViewModel by fetching the challenges data from Firestore.
    init {
        fetchChallenges()
    }

    // Fetches the list of challenges from Firestore for the currently authenticated user.
    private fun fetchChallenges() {
        val user = mAuth.currentUser
        if (user != null) {
            // Listen for real-time updates to the "challenges" collection in Firestore.
            db.collection("challenges")
                .addSnapshotListener { querySnapshot, e ->
                    if (e != null || querySnapshot == null) {
                        return@addSnapshotListener
                    }

                    // Get the locale language code for localizing challenge titles and descriptions.
                    val localeLanguageCode = getLocaleLanguageCode()

                    // Map Firestore documents to Challenge objects, translating content based on locale.
                    val challengesList = querySnapshot.documents.map { document ->
                        val titleMap = document.get("title") as? Map<String, String> ?: emptyMap()
                        val descriptionMap = document.get("description") as? Map<String, String> ?: emptyMap()
                        val evaluationCriteriaList = document.get("evaluationCriteria") as? List<Map<String, Any>> ?: emptyList()

                        // Create a Challenge object with localized content.
                        Challenge(
                            title = titleMap[localeLanguageCode] ?: titleMap["en"] ?: "",
                            description = descriptionMap[localeLanguageCode] ?: descriptionMap["en"] ?: "",
                            sdg = document.getString("sdg") ?: "",
                            startDate = document.getString("startDate") ?: "",
                            endDate = document.getString("endDate") ?: "",
                            active = document.getBoolean("active") ?: false,
                            evaluationCriteria = evaluationCriteriaList.map { criteriaMap ->
                                val criteriaMapTranslated = criteriaMap["criteria"] as? Map<String, String> ?: emptyMap()
                                EvaluationCriteria(
                                    criteria = criteriaMapTranslated[localeLanguageCode] ?: criteriaMapTranslated["en"] ?: "",
                                    maxPoints = (criteriaMap["maxPoints"] as? Number)?.toInt() ?: 5
                                )
                            },
                            id = document.id
                        )
                    }
                    // Update the StateFlow with the new list of challenges.
                    _challenges.value = challengesList
                }
        }
    }
}
