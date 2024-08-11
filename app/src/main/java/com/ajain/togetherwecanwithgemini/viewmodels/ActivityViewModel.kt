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

// ViewModel class managing the UI state and integrating with Google Gemini API for content generation.
class ActivityViewModel : BaseViewModel() {

    // StateFlow to handle UI states like loading, success, and error.
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Initialize Google Gemini API model with specified configuration.
    private val generativeModel = GenerativeModel(
        "gemini-1.5-flash",
        BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "text/plain"
        },
        systemInstruction = content { text("Be positive, no hatred. Empathize and motivate.") },
    )

    // Function to generate content using the Google Gemini API based on user activity details and a photo.
    fun geminifyContent(detail: String, bitmap: Bitmap) {
        _uiState.value = UiState.Loading  // Set UI state to loading
        val localLanguage = getLocaleLanguage()  // Fetch the local language from BaseViewModel

        // Launch a coroutine to perform the content generation asynchronously on the IO dispatcher.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create a prompt for the Google Gemini API including activity details and the local language.
                val prompt = """
                Below is the activity I performed. Analyze the activity and the photo and generate content for social media that I can post.
                Activity detail: "$detail"
                
                Answer in plaintext. Hashtags and emojis are allowed but not necessary. Do not give any extra information.
                Give output in "$localLanguage" language.
                """.trimIndent()

                // Call the Google Gemini API to generate content based on the provided prompt and image.
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt) }
                )
                
                // Update the UI state with the generated content if successful.
                response.text?.let { outputContent ->
                    _uiState.value = UiState.Success(outputContent)
                }
            } catch (e: Exception) {
                // Handle any errors during content generation and update the UI state accordingly.
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}
