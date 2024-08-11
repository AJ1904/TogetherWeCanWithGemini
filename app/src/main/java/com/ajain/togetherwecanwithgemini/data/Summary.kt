package com.ajain.togetherwecanwithgemini.data

import android.os.Parcel
import android.os.Parcelable

// Data class representing a summary with attributes for date, title, and summary content in multiple languages.
data class Summary(
    val date: String = "",
    val title: Map<String, String> = mapOf(),
    val summary: Map<String, String> = mapOf()
)

// Data class for a summary with content in a specific language.
data class SummaryWithLanguage(
    val title: String? = null,
    val summary: String? = null
)