package com.ajain.togetherwecanwithgemini.data

data class SDG(
    val name: Map<String, String> = mapOf(),
    val description: Map<String, String> = mapOf(),
    val index: Int = 0,
    val imageName: String = "",
    val sdgUrl: String = ""
)

