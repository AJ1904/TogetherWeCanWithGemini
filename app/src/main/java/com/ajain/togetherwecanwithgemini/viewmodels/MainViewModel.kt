package com.ajain.togetherwecanwithgemini.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import com.ajain.togetherwecanwithgemini.data.Activity
import com.ajain.togetherwecanwithgemini.data.AppDetailRepository
import com.ajain.togetherwecanwithgemini.data.Detail
import com.ajain.togetherwecanwithgemini.data.Goal
import com.ajain.togetherwecanwithgemini.data.Summary
import com.ajain.togetherwecanwithgemini.data.SummaryWithLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel() : BaseViewModel() {

    // Firebase Authentication and Firestore instances
    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val appDetailRepository = AppDetailRepository()

    // StateFlows to manage UI data
    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals

    private val _summary = MutableStateFlow<SummaryWithLanguage?>(null)
    val summary: StateFlow<SummaryWithLanguage?> = _summary

    // Initialize ViewModel by fetching necessary data
    init {
        fetchSummary(getLocaleLanguageCode())
        fetchGoals()
        fetchAppDetails()
    }

    // Firestore collection reference for activities
    private val firestore = FirebaseFirestore.getInstance()
    private val activitiesCollection = firestore.collection("activities")

    // StateFlow for paginated activities
    private val _activities = MutableStateFlow(getActivitiesPager())
    val activities: Flow<PagingData<Activity>> get() = _activities.value

    // StateFlow to manage app details
    private val _appDetails = MutableStateFlow<List<Detail>>(emptyList())
    val appDetails: StateFlow<List<Detail>> = _appDetails

    // Fetch app details from the repository
    private fun fetchAppDetails() {
        viewModelScope.launch {
            try {
                val details = appDetailRepository.getAppDetails()  // Fetch data
                _appDetails.value = details
            } catch (e: Exception) {
                // Handle any errors during fetching
                Log.d("line 68", e.toString())
            }
        }
    }

    // Fetch summary for today's date or the latest available
    private fun fetchSummary(languageCode: String) {
        val user = mAuth.currentUser
        if (user != null) {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Try to get today's summary
            db.collection("summaries").document(todayDate).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val summary = document.toObject(Summary::class.java)
                        if (summary != null) {
                            // Get title and summary in the specified language code
                            val title = (summary.title[languageCode] ?: summary.title["en"])?.replace(Regex("#"), "")?.trim()
                            val summaryText = summary.summary[languageCode] ?: summary.summary["en"]
                            _summary.value = SummaryWithLanguage(title, summaryText)
                        } else {
                            _summary.value = null
                        }
                    } else {
                        // Fetch the latest summary if today's is not available
                        fetchLatestSummary(languageCode)
                    }
                }
                .addOnFailureListener { e ->
                    _summary.value = null
                    Log.e("MainViewModel", "Error loading today's summary", e)
                }
        } else {
            _summary.value = null
        }
    }

    // Fetch the latest summary available in Firestore
    private fun fetchLatestSummary(languageCode: String) {
        db.collection("summaries")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val summary = document.toObject(Summary::class.java)
                    if (summary != null) {
                        // Get title and summary in the specified language code
                        val title = (summary.title[languageCode] ?: summary.title["en"])?.replace(Regex("#"), "")?.trim()
                        val summaryText = summary.summary[languageCode] ?: summary.summary["en"]
                        _summary.value = SummaryWithLanguage(title, summaryText)
                    } else {
                        _summary.value = null
                    }
                } else {
                    _summary.value = null
                }
            }
            .addOnFailureListener { e ->
                _summary.value = null
                Log.e("MainViewModel", "Error loading latest summary", e)
            }
    }

    // Fetch goals for the current user
    private fun fetchGoals() {
        val user = mAuth.currentUser
        if (user != null) {
            // Listen to changes in the user's goals collection
            db.collection("users").document(user.uid).collection("goals")
                .addSnapshotListener { querySnapshot, e ->
                    if (e != null || querySnapshot == null) {
                        return@addSnapshotListener
                    }

                    val goalsList = querySnapshot.documents.map { document ->
                        Goal(
                            title = document.getString("title") ?: "",
                            description = document.getString("description") ?: "",
                            sdg = document.getString("sdg") ?: "",
                            goalId = document.id
                        )
                    }
                    _goals.value = goalsList
                }
        }
    }

    // Refresh the activities list
    fun refreshActivities() {
        _activities.value = getActivitiesPager()
    }

    // Create a Pager for paginated activities
    private fun getActivitiesPager(): Flow<PagingData<Activity>> {
        return Pager(
            PagingConfig(pageSize = 1)
        ) {
            FirestorePagingSource(activitiesCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereEqualTo("isPublished", true)) // Only fetch published activities
        }.flow.cachedIn(viewModelScope)
    }

    // PagingSource implementation for Firestore
    class FirestorePagingSource(
        private val query: Query
    ) : PagingSource<QuerySnapshot, Activity>() {

        override fun getRefreshKey(state: PagingState<QuerySnapshot, Activity>): QuerySnapshot? =
            null

        override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Activity> {
            return try {
                val currentPage = params.key ?: query.limit(10).get().await()
                if (currentPage.isEmpty) {
                    // Handle empty page case
                    return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                }

                val lastVisibleDocument = currentPage.documents.last()
                val nextPage = query.startAfter(lastVisibleDocument).limit(10).get().await()

                // Map documents to Activity objects
                val activitiesList = currentPage.documents.map { document ->
                    document.toObject(Activity::class.java)?.copy(id = document.id)
                }.filterNotNull()

                LoadResult.Page(
                    data = activitiesList,
                    prevKey = null,
                    nextKey = nextPage
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    // Like an activity and update the like count
    fun likeActivity(activityId: String) {
        if (activityId.isEmpty()) {
            return
        }

        val activityRef = activitiesCollection.document(activityId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(activityRef)
            val currentLikes = snapshot.getLong("likeCount") ?: 0
            transaction.update(activityRef, "likeCount", currentLikes + 1)
        }.addOnSuccessListener {
            // Successfully updated the like count
            // Log.d("MainViewModel", "Updated like count.")
        }.addOnFailureListener { e ->
            // Log.e("MainViewModel", "Error updating like count", e)
        }
    }
}
