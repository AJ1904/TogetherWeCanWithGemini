package com.ajain.togetherwecanwithgemini.data

import java.util.Date

data class EvaluationCriteria(
    val criteria: String = "",
    val maxPoints: Int = 5
)

data class Challenge(
    val title: String = "",
    val description: String = "",
    val sdg: String = "",  // Field to indicate which SDG the goal is related to
//    val startDate: Date = Date("2024-01-01"),
//    val endDate: Date = Date("2099-12-31"),
    val startDate: String = "2024-01-01",
    val endDate: String = "2099-12-31",
    val active: Boolean = false,
    val evaluationCriteria: List<EvaluationCriteria> = emptyList(),
    val id: String = ""

    )

