package com.ajain.togetherwecanwithgemini.data
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Detail(
    val title: Map<String, String>,
    val body: Map<String, String>
)


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
