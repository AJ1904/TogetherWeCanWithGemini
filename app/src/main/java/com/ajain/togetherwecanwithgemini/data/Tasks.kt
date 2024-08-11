package com.ajain.togetherwecanwithgemini.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Tasks(
    val easyTasks: List<String>,
    val mediumTasks: List<String>,
    val difficultTasks: List<String>
)

fun parseTasks(jsonString: String): Tasks {
    return Json.decodeFromString(jsonString)
}
