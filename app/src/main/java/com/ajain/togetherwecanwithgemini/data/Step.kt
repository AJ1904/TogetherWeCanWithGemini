package com.ajain.togetherwecanwithgemini.data

// Data class representing a step in a process with attributes for ID, text, completion status, and an optional query.
data class Step(
    val id: String,
    val stepText: String,
    val completed: Boolean = false,
    val query: String = ""
)
