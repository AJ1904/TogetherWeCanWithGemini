package com.ajain.togetherwecanwithgemini

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Display the animated text within a centered box
@Composable
fun AnimatedText(textMap: Map<String, String>) {
    val languages = textMap.keys.toList()
    var currentLanguageIndex by remember { mutableStateOf(0) }
    val currentText = textMap[languages[currentLanguageIndex]] ?: textMap["en"] ?: ""

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Change text every 2 seconds
            currentLanguageIndex = (currentLanguageIndex + 1) % languages.size
        }
    }

    Box(
        modifier = Modifier
            .height(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = currentText, label = "") { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium,
            //    modifier = Modifier.height(48.dp)
            )
        }
    }
}
