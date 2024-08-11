package com.ajain.togetherwecanwithgemini.data

// Data class representing a Sustainable Development Goal (SDG) with attributes for name, description, index, image, and URL.
data class SDG(
    val name: Map<String, String> = mapOf(),
    val description: Map<String, String> = mapOf(),
    val index: Int = 0,
    val imageName: String = "",
    val sdgUrl: String = ""
)

