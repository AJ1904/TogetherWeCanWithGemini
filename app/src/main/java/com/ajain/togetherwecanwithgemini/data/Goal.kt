package com.ajain.togetherwecanwithgemini.data

// Data class representing a user goal with attributes for title, description, related SDG, and a unique identifier.
data class Goal(
    val title: String = "",
    val description: String = "",
    val sdg: String = "",  // Field to indicate which SDG the goal is related to
    val goalId: String = ""
)
