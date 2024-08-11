package com.ajain.togetherwecanwithgemini.data

import android.os.Parcel
import android.os.Parcelable


data class Summary(
    val date: String = "",
    val title: Map<String, String> = mapOf(),
    val summary: Map<String, String> = mapOf()
)
data class SummaryWithLanguage(
    val title: String? = null,
    val summary: String? = null
)