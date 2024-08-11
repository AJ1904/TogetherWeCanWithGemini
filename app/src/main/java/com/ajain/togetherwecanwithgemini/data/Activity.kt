package com.ajain.togetherwecanwithgemini.data

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
