package com.ajain.togetherwecanwithgemini.data

// Data class representing a quiz question with its text, correct option, list of options, and hints.
data class QuizQuestion(
    val question: String,
    val correctOption: String,
    val options: List<String>,
    val hints: List<String>
)
