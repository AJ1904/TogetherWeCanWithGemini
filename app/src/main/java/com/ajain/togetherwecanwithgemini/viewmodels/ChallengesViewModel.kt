package com.ajain.togetherwecanwithgemini.viewmodels

import androidx.lifecycle.ViewModel
import com.ajain.togetherwecanwithgemini.data.Challenge
import com.ajain.togetherwecanwithgemini.data.EvaluationCriteria
import com.ajain.togetherwecanwithgemini.data.Goal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.getField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date


class ChallengesViewModel : BaseViewModel() {
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _challenges = MutableStateFlow<List<Challenge>>(emptyList())
    val challenges: StateFlow<List<Challenge>> = _challenges

    init {
        fetchChallenges()
    }

    private fun fetchChallenges() {
        val user = mAuth.currentUser
        if (user != null) {

            db.collection("challenges")
                .addSnapshotListener { querySnapshot, e ->
                    if (e != null || querySnapshot == null) {
                        return@addSnapshotListener
                    }

                    val localeLanguageCode = getLocaleLanguageCode()
                    val challengesList = querySnapshot.documents.map { document ->
                        // val evaluationCriteria = document.get("evaluationCriteria") as? List<Map<String, Any>> ?: emptyList()
                        val titleMap = document.get("title") as? Map<String, String> ?: emptyMap()
                        val descriptionMap = document.get("description") as? Map<String, String> ?: emptyMap()
                        val evaluationCriteriaList = document.get("evaluationCriteria") as? List<Map<String, Any>> ?: emptyList()

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
                    _challenges.value = challengesList
                }


        }

    }
}
