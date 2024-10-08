package com.ajain.togetherwecanwithgemini.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ajain.togetherwecanwithgemini.BuildConfig
import com.ajain.togetherwecanwithgemini.UiState
import com.ajain.togetherwecanwithgemini.data.QuizQuestion
import com.ajain.togetherwecanwithgemini.data.SDG
import com.ajain.togetherwecanwithgemini.data.Step
import com.ajain.togetherwecanwithgemini.data.Tasks
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ajain.togetherwecanwithgemini.data.Detail
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// ViewModel class for managing SDG (Sustainable Development Goals) related data and interactions.
class SDGViewModel(private val savedStateHandle: SavedStateHandle) : BaseViewModel() {

    companion object {
        private const val KEY_TASKS = "tasks"
    }

    // StateFlow to manage SDG details.
    private val _sdgDetails: MutableStateFlow<List<Detail>> = MutableStateFlow(emptyList())
    val sdgDetails: StateFlow<List<Detail>> = _sdgDetails.asStateFlow()

    // StateFlow to manage UI states like loading, success, and error.
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Property to handle tasks using SavedStateHandle.
    private var _tasks: Tasks?
        get() = savedStateHandle[KEY_TASKS]
        set(value) {
            savedStateHandle[KEY_TASKS] = value
        }
    val tasks: Tasks?
        get() = _tasks

    // StateFlow to manage steps and their related data.
    private val _steps: MutableStateFlow<List<Step>?> = MutableStateFlow(null)
    val steps: StateFlow<List<Step>?> = _steps.asStateFlow()

    // StateFlow to manage Firestore steps data.
    private val _firestoreSteps: MutableStateFlow<List<Step>?> = MutableStateFlow(null)
    val firestoreSteps: StateFlow<List<Step>?> = _firestoreSteps.asStateFlow()

    // LiveData to manage quiz questions.
    private val _quizQuestions = MutableLiveData<List<QuizQuestion>?>()
    val quizQuestions: LiveData<List<QuizQuestion>?> = _quizQuestions

    // StateFlow to manage UI states for quiz-related operations.
    private val _uiStateQuiz: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiStateQuiz: StateFlow<UiState> = _uiStateQuiz.asStateFlow()

    // Local language code for generating content in the user's preferred language.
    val localLanguage = getLocaleLanguage()

    // Set the Firestore steps list.
    fun setFirestoreSteps(stepsList: List<Step>) {
        _firestoreSteps.value = stepsList
    }

    // Initialize Gemini API model for generating SDG-related content.
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "application/json"
        },
        systemInstruction = content { text("Keep it straightforward and easy to understand.") },
    )

    // Function to generate tasks for a given SDG using the Gemini API.
    fun getActionsForSDG(sdgName: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                Based on SDG Goal $sdgName, suggest 5 easy-to-do tasks, 5 medium difficulty tasks, and 5 difficult tasks that I can perform to contribute to the $sdgName.

                The JSON response must be formatted as follows:
                {
                  "easyTasks": ["Heading 1: Description", "Heading 2: Description"...],
                  "mediumTasks": ["Heading 1: Description", "Heading 2: Description"...],
                  "difficultTasks": ["Heading 1: Description", "Heading 2: Description"...]
                }
                
                Give output in "$localLanguage" language.
                """.trimIndent()

                // Generate tasks using the Gemini API.
                val response = generativeModel.generateContent(
                    content { text(prompt) }
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

    // Function to suggest steps for achieving a specific goal.
    fun suggestStepsForGoal(goalTitle: String, goalDescription: String, sdg: String, location: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var prompt = """
                I have a goal titled "$goalTitle" with the description "$goalDescription". Please suggest at most 10 steps that can help me achieve this goal.

                The JSON response must be formatted as follows:
                {
                  "steps": ["step 1...", "step 2...", ...]
                }
                """.trimIndent()
                if (location != ""){
                    prompt = """
                I have a goal titled "$goalTitle" with the description "$goalDescription". Please suggest at most 10 steps that can help me achieve this goal. If required, you can use my location "$location".

                The JSON response must be formatted as follows:
                {
                  "steps": ["step 1...", "step 2...", ...]
                }
                Give output in "$localLanguage" language.
                
                """.trimIndent()
                }

                // Generate steps using the Gemini API.
                val response = generativeModel.generateContent(
                    content { text(prompt) }
                )
                response.text?.let { Log.d("Gemini", it) }
                response.text?.let { outputContent ->
                    val stepsList = parseSteps(outputContent)
                    _steps.value = stepsList
                    _uiState.value = UiState.Success(outputContent)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    // Function to parse the steps from the JSON response.
    private fun parseSteps(json: String): List<Step> {
        val jsonObject = Gson().fromJson(json, JsonObject::class.java)
        return jsonObject.getAsJsonArray("steps").map {
            Step(id = "", stepText = it.asString, completed = false)
        }
    }

    // Function to determine if a step is related to Google Maps searches.
    fun isThisStepRelatedToMaps(step: String, onResult: (String) -> Unit) {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                "$step" Is this a task for which I can search on Google maps for locations? If yes, output a JSON object with {"query": "..."} for the Google maps API. If no, output {"query": "NO"}. Don't add any extra explanation.
                """.trimIndent()

                // Check if the step is related to Google Maps using the Gemini API.
                val response = generativeModel.generateContent(
                    content { text(prompt) }
                )
                response.text?.let { Log.d("Gemini", it) }
                response.text?.let { outputContent ->
                    val jsonObject = Gson().fromJson(outputContent, JsonObject::class.java)
                    val query = jsonObject.get("query").asString
                    onResult(query)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
                e.localizedMessage?.let { Log.e("Gemini error:", it) }
            }
        }
    }

    // Initialize Gemini API model for generating quiz questions.
    private val quizGenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 64
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "application/json"
        },
        systemInstruction = content { text("Easy to understand.") },
    )

    // Function to clear the quiz state.
    private fun clearQuizState() {
        _quizQuestions.postValue(emptyList())  // Clear quiz questions.
        _uiStateQuiz.value = UiState.Initial   // Reset UI state for the quiz.
    }

    // Function to generate quiz questions based on a given SDG.
    fun generateQuizQuestion(sdg: SDG) {
        clearQuizState()
        _uiStateQuiz.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _quizQuestions.postValue(emptyList())
                val prompt = """
                Generate 5 multiple-choice question, answer and explanation for each option for "$sdg".
                
                The JSON response must be formatted as follows:
                {
                  "q1": {
                      "correctOption": "option...",
                      "options": {
                      "option1": "...",
                      "option2": "...",
                      "option3": "...",
                      "option4": "..."
                      },
                      "question": "...",
                      "explanations": {
                      "e1": "...",
                      "e2": "...",
                      "e3": "...",
                      "e4": "..."
                      }
                     },
                  "q2": {
                      "correctOption": "option...",
                      "options": {
                      "option1": "...",
                      "option2": "...",
                      "option3": "...",
                      "option4": "..."
                      },
                      "question": "...",
                      "explanations": {
                      "e1": "...",
                      "e2": "...",
                      "e3": "...",
                      "e4": "..."
                      }
                     },
                  ...
                }

                Give output in "$localLanguage" language.

            """.trimIndent()

                // Generate quiz questions using the Gemini API.
                val response = quizGenerativeModel.generateContent(
                    content { text(prompt) }
                )
                response.text?.let { Log.d("Gemini", it) }
                response.text?.let { outputContent ->
                    _uiStateQuiz.value = UiState.Success(outputContent)
                    val jsonObject = Gson().fromJson(outputContent, JsonObject::class.java)

                    val quizQuestions = mutableListOf<QuizQuestion>()
                    jsonObject.entrySet().forEach { entry ->
                        val questionObject = entry.value.asJsonObject
                        val questionText = questionObject.get("question").asString
                        val optionsJsonObject = questionObject.getAsJsonObject("options")
                        val optionsMap = optionsJsonObject.entrySet().associate {
                            it.key to it.value.asString
                        }
                        val hintsJsonObject = questionObject.getAsJsonObject("explanations")

                        val correctOptionIdentifier = questionObject.get("correctOption").asString
                        val correctOption = optionsMap[correctOptionIdentifier] ?: ""

                        val options = optionsJsonObject.entrySet().map { it.value.asString }
                        val hints = hintsJsonObject.entrySet().map { it.value.asString }

                        quizQuestions.add(QuizQuestion(questionText, correctOption, options, hints))
                    }

                    _quizQuestions.postValue(quizQuestions)
                }
            } catch (e: Exception) {
                _uiStateQuiz.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }

    // Function to fetch SDG details from Firestore based on the provided SDG ID.
    fun getSDGDetails(sdgId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                // Find the document in the "sdg" collection where the "index" field matches sdgId.
                val sdgCollection = db.collection("sdg")
                val querySnapshot = sdgCollection.whereEqualTo("index", sdgId).get().await()

                if (!querySnapshot.isEmpty) {
                    val sdgDoc = querySnapshot.documents.first()
                    val detailsCollection = sdgDoc.reference.collection("details")

                    // Fetch details from the "details" subcollection.
                    val detailsSnapshot = detailsCollection.get().await()
                    val detailList = detailsSnapshot.map { doc ->
                        val data = doc.data
                        val title = data["title"] as? Map<String, String>
                        val body = data["body"] as? Map<String, String>
                        Detail(title = title ?: emptyMap(), body = body ?: emptyMap())
                    }

                    // Sort details by the length of the title or body values.
                    val sortedDetailList = detailList.sortedBy { detail ->
                        detail.title.values.firstOrNull()?.length ?: 0
                    }
                    _sdgDetails.value = sortedDetailList
                    Log.d("line 279", _sdgDetails.toString())
                } else {
                    Log.e("SDGDetailsError", "No document found for index: $sdgId")
                }
            } catch (e: Exception) {
                // Handle error
                Log.e("SDGDetailsError", "Error fetching SDG details", e)
            }
        }
    }
}
