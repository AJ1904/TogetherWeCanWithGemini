package com.ajain.togetherwecanwithgemini.data

data class Step(
    val id: String,
    val stepText: String,
    val completed: Boolean = false,
    val query: String = ""
)
