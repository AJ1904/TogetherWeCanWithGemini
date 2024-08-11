package com.ajain.togetherwecanwithgemini.data
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Data class representing the details of an app, including title and body information in multiple languages.
data class Detail(
    val title: Map<String, String>,
    val body: Map<String, String>
)

// Repository class for fetching app details from Firestore.
class AppDetailRepository {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getAppDetails(): List<Detail> {
        return try {
            val snapshot = firestore.collection("appDetails").get().await()
            snapshot.documents.map { doc ->
                val title = doc.get("title") as Map<String, String>
                val body = doc.get("body") as Map<String, String>
                Detail(title = title, body = body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
