package com.ajain.togetherwecanwithgemini.viewmodels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ajain.togetherwecanwithgemini.BuildConfig
import com.ajain.togetherwecanwithgemini.UiState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ActivityViewModel : BaseViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        "gemini-1.5-flash",
        // Retrieve API key as an environmental variable defined in a Build Configuration
        // see https://github.com/google/secrets-gradle-plugin for further instructions
        BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "text/plain"
        },
        // safetySettings = Adjust safety settings
        // See https://ai.google.dev/gemini-api/docs/safety-settings
        systemInstruction = content { text("Be positive, no hatred. Empathize and motivate.") },
    )


    fun geminifyContent(detail: String, bitmap: Bitmap) {
        _uiState.value = UiState.Loading
        //val localLanguage = "Hindi"
        val localLanguage = getLocaleLanguage()  // Use getLocaleLanguage from BaseViewModel

        // Log.d("language", localLanguage)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                Below is the activity I performed. Analyze the activity and the photo and generate content for social media that I can post.
                Activity detail: "$detail"
                
                Answer in plaintext. Hashtags and emojis are allowed but not necessary. Do not give any extra information.
                Give output in "$localLanguage" language.
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt) }
                )
                response.text?.let { Log.d("Gemini", it) }
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}
