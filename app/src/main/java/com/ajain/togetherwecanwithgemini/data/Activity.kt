package com.ajain.togetherwecanwithgemini.data

// Data class representing an activity with attributes for details, associated goal, image, timestamp, user information, and publication status.
data class Activity(
    val detail: String = "",
    val goalTitle: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0,
    val userId: String = "",
    val id: String = "",
    val userDisplayName: String = "",
    var isPublished: Boolean = false,
    val likeCount: Long? = 0,
    val goalId: String = ""

)
