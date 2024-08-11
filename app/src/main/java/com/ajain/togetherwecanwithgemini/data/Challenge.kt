package com.ajain.togetherwecanwithgemini.data

import java.util.Date

// Data class representing evaluation criteria for a challenge, including criteria description and maximum points.
data class EvaluationCriteria(
    val criteria: String = "",
    val maxPoints: Int = 5
)

// Data class representing a challenge with attributes for title, description, related SDG, date range, status, evaluation criteria, and unique identifier.
data class Challenge(
    val title: String = "",
    val description: String = "",
    val sdg: String = "",  // Field to indicate which SDG the goal is related to
    val startDate: String = "2024-01-01",
    val endDate: String = "2099-12-31",
    val active: Boolean = false,
    val evaluationCriteria: List<EvaluationCriteria> = emptyList(),
    val id: String = ""

    )

