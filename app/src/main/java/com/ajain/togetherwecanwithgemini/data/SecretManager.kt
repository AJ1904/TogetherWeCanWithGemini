package com.ajain.togetherwecanwithgemini.data

//import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
//
//object SecretManager {
//    private val secretManagerClient = SecretManagerServiceClient.create()
//
//    fun getGeminiApiKey(): String {
//        val secretName = "projects/175255734031/secrets/GEMINI_API_KEY/versions/latest"
//        val accessSecretVersionResponse = secretManagerClient.accessSecretVersion(secretName)
//        return accessSecretVersionResponse.payload.data.toStringUtf8()
//    }
//
//    fun getGoogleMapsApiKey(): String {
//        val secretName = "projects/175255734031/secrets//versions/latest"
//        val accessSecretVersionResponse = secretManagerClient.accessSecretVersion(secretName)
//        return accessSecretVersionResponse.payload.data.toStringUtf8()
//    }
//
//}
