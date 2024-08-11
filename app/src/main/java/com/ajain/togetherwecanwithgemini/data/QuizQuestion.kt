package com.ajain.togetherwecanwithgemini.data

data class QuizQuestion(
    val question: String,
    val correctOption: String,
    val options: List<String>,
    val hints: List<String>
)
