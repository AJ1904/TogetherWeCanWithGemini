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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainViewModel() : BaseViewModel() {

    private val mAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val appDetailRepository = AppDetailRepository()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals

    private val _summary = MutableStateFlow<SummaryWithLanguage?>(null)
    val summary: StateFlow<SummaryWithLanguage?> = _summary

    init {
        fetchSummary(getLocaleLanguageCode())
        fetchGoals()
        fetchAppDetails()
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val activitiesCollection = firestore.collection("activities")

    private val _activities = MutableStateFlow(getActivitiesPager())
    val activities: Flow<PagingData<Activity>> get() = _activities.value

  //  private val appDetailRepository = AppDetailRepository()
    private val _appDetails = MutableStateFlow<List<Detail>>(emptyList())
    val appDetails: StateFlow<List<Detail>> = _appDetails

    private fun fetchAppDetails() {
        viewModelScope.launch {
            try {
                val details = appDetailRepository.getAppDetails()  // Fetch data
                _appDetails.value = details
                // Handle the fetched details
            } catch (e: Exception) {
                // Handle the error
                Log.d("line 68", e.toString())
            }
        }
    }
//    private fun fetchSummary() {
//        // Fetch summary from Firestore or other source and update _summary
//        val user = mAuth.currentUser
//        if (user != null) {
//            val date = "2024-08-02" //SimpleDateFormat("yyyy-MM-dd").format(Date()) //"2024-08-02" // Adjust as needed
//            db.collection("summaries").document(date).get()
//                .addOnSuccessListener { document ->
//                    if (document != null && document.exists()) {
//                        val summary = document.toObject(Summary::class.java)
//                        _summary.value = summary
//                    } else {
//                        _summary.value = null
//                    }
//                }
//                .addOnFailureListener { e ->
//                    _summary.value = null
//                    Log.e("MainViewModel", "Error loading summary", e)
//                }
//        } else {
//            _summary.value = null
//        }
//    }
//private fun fetchSummary() {
//    val user = mAuth.currentUser
//    if (user != null) {
//        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//
//        // Try to get today's summary
//        db.collection("summaries").document(todayDate).get()
//            .addOnSuccessListener { document ->
//                if (document != null && document.exists()) {
//                    // If today's summary exists, update _summary
//                    val summary = document.toObject(Summary::class.java)
//                    _summary.value = summary
//                } else {
//                    // If today's summary doesn't exist, fetch the latest summary
//                    fetchLatestSummary()
//                }
//            }
//            .addOnFailureListener { e ->
//                _summary.value = null
//                Log.e("MainViewModel", "Error loading today's summary", e)
//            }
//    } else {
//        _summary.value = null
//    }
//}
//
//    private fun fetchLatestSummary() {
//        db.collection("summaries")
//            .orderBy("date", Query.Direction.DESCENDING)
//            .limit(1)
//            .get()
//            .addOnSuccessListener { querySnapshot ->
//                if (!querySnapshot.isEmpty) {
//                    val document = querySnapshot.documents[0]
//                    val summary = document.toObject(Summary::class.java)
//                    _summary.value = summary
//                } else {
//                    _summary.value = null
//                }
//            }
//            .addOnFailureListener { e ->
//                _summary.value = null
//                Log.e("MainViewModel", "Error loading latest summary", e)
//            }
//    }
//
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
                    // If today's summary doesn't exist, fetch the latest summary
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
                        val title = (summary.title[languageCode] ?: summary.title["en"])?.replace(Regex("#"), "")?.trim() //(summary.title[languageCode] ?: summary.title["en"])?.trimStart('#')
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



    private fun fetchGoals() {
        val user = mAuth.currentUser
        if (user != null) {
            // Fetch goals from subcollection
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
//                            progress = document.getLong("progress") ?: 0L,
                            goalId = document.id
                        )
                    }
                    _goals.value = goalsList
                }


        }
    }

    fun refreshActivities() {
        _activities.value = getActivitiesPager()
    }

    private fun getActivitiesPager(): Flow<PagingData<Activity>> {
        return Pager(
            PagingConfig(pageSize = 1)
        ) {
            //FirestorePagingSource(activitiesCollection.orderBy("timestamp", Query.Direction.DESCENDING))
            FirestorePagingSource(activitiesCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereEqualTo("isPublished", true)) // Only fetch published activities
        }.flow.cachedIn(viewModelScope)
    }

    class FirestorePagingSource(
        private val query: Query
    ) : PagingSource<QuerySnapshot, Activity>() {

        override fun getRefreshKey(state: PagingState<QuerySnapshot, Activity>): QuerySnapshot? =
            null

        override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Activity> {
            return try {
                val currentPage = params.key ?: query.limit(10).get().await()
                if (currentPage.isEmpty) {
                    // Handle empty page case (e.g., return an empty page or stop further loading)
                    return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                }

                val lastVisibleDocument = currentPage.documents.last()

                val nextPage = query.startAfter(lastVisibleDocument).limit(10).get().await()

                // Map documents to Activity objects, including the document ID
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


fun likeActivity(activityId: String) {
    if (activityId.isEmpty()) {
        Log.e("MainViewModel", "Activity ID is empty")
        return
    }

    val activityRef = activitiesCollection.document(activityId)
    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(activityRef)
        val currentLikes = snapshot.getLong("likeCount") ?: 0
        transaction.update(activityRef, "likeCount", currentLikes + 1)
    }.addOnSuccessListener {
        // Successfully updated the like count
        Log.e("MainViewModel", "Updated like count.", )
    }.addOnFailureListener { e ->
        Log.e("MainViewModel", "Error updating like count", e)
    }
}


}
