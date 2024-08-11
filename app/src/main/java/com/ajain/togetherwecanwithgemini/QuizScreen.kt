package com.ajain.togetherwecanwithgemini

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ajain.togetherwecanwithgemini.data.QuizQuestion
import com.ajain.togetherwecanwithgemini.ui.theme.Blue64
import com.ajain.togetherwecanwithgemini.ui.theme.Orange40
import com.ajain.togetherwecanwithgemini.ui.theme.Pink40

@Composable
fun QuizScreen(
    question: QuizQuestion?,
    onNextQuestion: () -> Unit,
    onEndQuiz: () -> Unit
) {
    question?.let {
        var selectedOption by remember { mutableStateOf<String?>(null) }
        var correctAnswered by remember { mutableStateOf(false) }
        var optionColors by remember { mutableStateOf(it.options.associateWith { Blue64 }) }
        var currentHint by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            Text(text = it.question,
                style = MaterialTheme.typography.headlineMedium,
              //  modifier = Modifier.padding(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            it.options.forEachIndexed { index, option ->
                val color = optionColors[option] ?: Blue64
                val enabled = true

                Button(
                    onClick = {
                        if (enabled) {
                            selectedOption = option
                            correctAnswered = (option == it.correctOption)
                            currentHint = it.hints.getOrNull(index)

                            optionColors = it.options.associateWith { opt ->
                                when {
                                    opt == it.correctOption && opt == selectedOption -> Orange40
                                    opt != it.correctOption && opt == selectedOption -> Pink40
                                    else -> optionColors[opt] ?: Blue64
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    enabled = enabled
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        // horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(text = option,
                            style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(0.9f).padding(8.dp))
                        if (selectedOption == option) {
                            Icon(
                                imageVector = if (correctAnswered) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {
                        optionColors = it.options.associateWith { Blue64 }
                        selectedOption = null
                        correctAnswered = false
                        currentHint = null
                        onNextQuestion()
                    },
                    enabled = correctAnswered,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(text = stringResource(R.string.next_question))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onEndQuiz,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(text = stringResource(R.string.end_quiz))
                }
            }
            currentHint?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } ?: run {
        LoadingIndicator()
    }
}
