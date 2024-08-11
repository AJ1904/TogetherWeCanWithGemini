package com.ajain.togetherwecanwithgemini.data

data class Goal(
    val title: String = "",
    val description: String = "",
    val sdg: String = "",  // Field to indicate which SDG the goal is related to
//    val progress: Long = 0,
    val goalId: String = ""
)
