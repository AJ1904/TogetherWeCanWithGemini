package com.ajain.togetherwecanwithgemini

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajain.togetherwecanwithgemini.data.Goal
import com.ajain.togetherwecanwithgemini.data.SDG
import com.ajain.togetherwecanwithgemini.data.Tasks
import com.ajain.togetherwecanwithgemini.data.parseTasks
import com.ajain.togetherwecanwithgemini.utils.loadSDGs
import com.ajain.togetherwecanwithgemini.viewmodels.SDGViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun LearnScreen(navController: androidx.navigation.NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sdgs = remember { loadSDGs(context) }
    SDGCardView(sdgs = sdgs, navController = navController, modifier = modifier)
}

@Composable
fun SDGCardView(sdgs: List<SDG>, navController: androidx.navigation.NavController, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sdgs) { sdg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    //.fillMaxHeight()
                    .clickable {
                        val gson = Gson()
                        val encodedName = URLEncoder
                            .encode(gson.toJson(sdg.name), StandardCharsets.UTF_8.toString())
                            .replace("+", "%20")
                        val encodedDescription = URLEncoder
                            .encode(gson.toJson(sdg.description), StandardCharsets.UTF_8.toString())
                            .replace("+", "%20")
                        val encodedSdgUrl = URLEncoder
                            .encode(sdg.sdgUrl, StandardCharsets.UTF_8.toString())
                            .replace("+", "%20")

                        // Navigate to the detail screen
                        navController.navigate(
                            "sdg_detail/$encodedName/$encodedDescription/${sdg.index}/${sdg.imageName}/$encodedSdgUrl"
                        )
                    }
            ) {
                Image(
                    painter = painterResource(
                        id = LocalContext.current.resources.getIdentifier(
                            sdg.imageName, "drawable", LocalContext.current.packageName
                        )
                    ),
                    contentDescription = sdg.name["en"],
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}



@Composable
fun SDGDetailScreen(sdg: SDG, sdgViewModel: SDGViewModel = viewModel()) {
    val uiState by sdgViewModel.uiState.collectAsState()
    val sdgDetails by sdgViewModel.sdgDetails.collectAsState()
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<Tasks?>(null) }
    var buttonEnabled by remember { mutableStateOf(true) }
  //  var buttonText by remember { mutableStateOf("Get Actions") }

    // Separate expanded task indices for each level
    var expandedEasyTaskIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var expandedMediumTaskIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var expandedHardTaskIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val uiStateQuiz by sdgViewModel.uiStateQuiz.collectAsState()
    val quizQuestions by sdgViewModel.quizQuestions.observeAsState(emptyList())
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var showQuiz by remember { mutableStateOf(false) }

    // State for showing dialog
    var showSourceDialog by remember { mutableStateOf(false) }

    var showTasksDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    val localLanguageCode = sdgViewModel.getLocaleLanguageCode()

    // Retrieve tasks from ViewModel if already generated
    tasks = sdgViewModel.tasks

    LaunchedEffect(sdg.index) {
        sdgViewModel.getSDGDetails(sdg.index)
    }

    LaunchedEffect(uiStateQuiz) {
        when (uiStateQuiz) {
            is UiState.Loading -> {
                // Show loading indicator
                loading = true
                errorMessage = null
                //CircularProgressIndicator()
            }

            is UiState.Error -> {
                // Show error message
                // Text(text = "Error: ${(uiStateQuiz as UiState.Error).errorMessage}")
                loading = false
                errorMessage = (uiStateQuiz as UiState.Error).errorMessage
            }

            is UiState.Success -> {
//                showQuiz = true
                loading = false
                errorMessage = null
                showQuiz = true
                currentQuestionIndex = 0
            }

            else -> {
                // Initial or other state
                loading = false
                errorMessage = null
            }
        }
    }

    if (showQuiz) {
        QuizScreen(
            question = quizQuestions?.getOrNull(currentQuestionIndex),
            onNextQuestion = {
                if (currentQuestionIndex < (quizQuestions?.size ?: 0) - 1) {
                    currentQuestionIndex++
                } else {
                    // Handle end of quiz logic here
                    showQuiz = false
                }
            },
            onEndQuiz = {
                showQuiz = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .systemBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Image(
                    painter = painterResource(
                        id = context.resources.getIdentifier(
                            sdg.imageName, "drawable", context.packageName
                        )
                    ),
                    contentDescription = sdg.name[localLanguageCode] ?: sdg.name["en"],
                    modifier = Modifier.fillMaxWidth(0.4f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

               //     Spacer(modifier = Modifier.height(4.dp))
                    //Row {
                        Button(
                            onClick = {
                                buttonEnabled = false
                                showTasksDialog = true
                                sdg.name["en"]?.let { sdgViewModel.getActionsForSDG(it, ) }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor= MaterialTheme.colorScheme.secondary,
                                contentColor= MaterialTheme.colorScheme.onSecondary,
                                disabledContainerColor= MaterialTheme.colorScheme.primary,
                                disabledContentColor= MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = buttonEnabled,

                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(R.string.get_actions))
                        }
                       // Spacer(modifier = Modifier.width(8.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                        Button(onClick = {
                            sdgViewModel.generateQuizQuestion(sdg)
                            showQuiz = true
                        },
                            colors = ButtonDefaults.buttonColors(
                                containerColor= MaterialTheme.colorScheme.secondary,
                                contentColor= MaterialTheme.colorScheme.onSecondary,
                                disabledContainerColor= MaterialTheme.colorScheme.primary,
                                disabledContentColor= MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(R.string.quiz))
                        }
                        Spacer(modifier = Modifier.height(4.dp))


                    //}
                    Button(onClick = {
                        showSourceDialog = true  // Show the dialog
                    },
                        colors = ButtonDefaults.buttonColors(
                            containerColor= MaterialTheme.colorScheme.secondary,
                            contentColor= MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor= MaterialTheme.colorScheme.primary,
                            disabledContentColor= MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.source_un))
                    }

                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            AnimatedText(textMap = sdg.name)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = sdg.description[localLanguageCode] ?: sdg.description["en"] ?: "",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            // Carousel below the description
            if (sdgDetails.isNotEmpty()) {
                Carousel(details = sdgDetails,
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        //.fillMaxWidth()
                    ,
                    localLanguageCode = sdgViewModel.getLocaleLanguageCode()
                )
            } //else {
               // Text("No details available", style = MaterialTheme.typography.bodyMedium)
            //}

            Spacer(modifier = Modifier.height(16.dp))


        }
    }

    if (showSourceDialog) {
        Dialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSourceDialog = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.9f),
                        color = MaterialTheme.colorScheme.background
            ) {
                Box {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.info),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider()
                        WebViewPage(url = sdg.sdgUrl)
                        Button(
                            onClick = { showSourceDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(text = stringResource(R.string.close))
                        }
                    }
                    IconButton(
                        onClick = { showSourceDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(0.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            }
        }
    }
        if (showTasksDialog) {
            Dialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { showTasksDialog = false }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        //.fillMaxSize()
                        .fillMaxHeight(0.9f)
                        //.height(400.dp)
                        // .background(color = MaterialTheme.colorScheme.background)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (uiState) {
                        is UiState.Loading -> {
                           // CircularProgressIndicator()
                            LoadingIndicator()
                        }

                        is UiState.Success -> {
                            val json = (uiState as UiState.Success).outputText
                            try{

                                if (tasks == null) {
                                    tasks = parseTasks(json)
                                }
                            }catch (e: Exception) {
                                Log.d("LearnScreen","Something went wrong, try again")
                                AlertDialog(onDismissRequest = { showTasksDialog=false }, confirmButton = { })
                            }
                           // buttonText = stringResource(R.string.get_more_actions)
                            buttonEnabled = true
                        }

                        is UiState.Error -> {
                            Text(
                                text = (uiState as UiState.Error).errorMessage,
                                color = MaterialTheme.colorScheme.error
                            )
                            buttonEnabled = true
                        }

                        else -> {}
                    }

                    tasks?.let { taskList ->
                        LazyColumn(modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))) {
                            item {
                                TaskSection(
                                    stringResource(R.string.easy_tasks),
                                    taskList.easyTasks,
                                    MaterialTheme.colorScheme.primary,
                                    expandedEasyTaskIndex,
                                    sdg.name["en"]?: ""
                                ) { index ->
                                    expandedEasyTaskIndex = index
                                }
                            }

                            item {
                                TaskSection(
                                    stringResource(R.string.medium_tasks),
                                    taskList.mediumTasks,
                                    MaterialTheme.colorScheme.primary,
                                    expandedMediumTaskIndex,
                                    sdg.name["en"]?: ""
                                ) { index ->
                                    expandedMediumTaskIndex = index
                                }
                            }
                            item {
                                TaskSection(
                                    stringResource(R.string.difficult_tasks),
                                    taskList.difficultTasks,
                                    MaterialTheme.colorScheme.primary,
                                    expandedHardTaskIndex,
                                    sdg.name["en"]?: ""
                                ) { index ->
                                    expandedHardTaskIndex = index
                                }
                            }
                        }
                    }
                }
            }
    }
}

fun saveGoalToFirebase(
    title: String,
    description: String,
    sdg: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    val user = mAuth.currentUser
    if (user != null) {
        val goal = Goal(title, description, sdg)
        db.collection("users").document(user.uid)
            .collection("goals").document(title).set(goal)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}


