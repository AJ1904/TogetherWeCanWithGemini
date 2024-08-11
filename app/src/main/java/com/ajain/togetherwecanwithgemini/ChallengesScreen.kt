package com.ajain.togetherwecanwithgemini

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ajain.togetherwecanwithgemini.data.Challenge
import com.ajain.togetherwecanwithgemini.data.Goal
import com.ajain.togetherwecanwithgemini.goals.GoalDetailActivity
import com.ajain.togetherwecanwithgemini.utils.loadSDGs
import com.ajain.togetherwecanwithgemini.viewmodels.ChallengesViewModel
import com.ajain.togetherwecanwithgemini.viewmodels.MainViewModel


@Composable
fun ChallengesScreen(
    viewModel: ChallengesViewModel, navController: NavController
) {
    val challengesState = viewModel.challenges.collectAsState()
    val challenges = challengesState.value
    val localLanguageCode = viewModel.getLocaleLanguageCode()
    Box(
        modifier = Modifier.fillMaxSize().systemBarsPadding() ,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.challenges_screen),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.padding(bottom = 80.dp) // Adjust the bottom padding as needed
            ) {
                items(challenges) { challenge ->
                    ChallengeItem(challenge = challenge, navController = navController, localLanguageCode = localLanguageCode)
                }
            }
        }
    }
}

@Composable
fun ChallengeItem(challenge: Challenge, navController: NavController, localLanguageCode: String) {
    val context = LocalContext.current
    val sdgs = remember { loadSDGs(context) }
    val sdgNumber = challenge.sdg.substringAfter("SDG ").substringBefore(":").toIntOrNull()
    val sdg = sdgNumber?.let { number -> sdgs.find { it.index == number } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha=0.9f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { navController.navigate("challenge_detail/${challenge.id}") }
            .padding(16.dp)
    ) {
        // Column to hold the title and image
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            // Row to align the title and image
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start)
            ) {
                // Title
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )

                // SDG Image
                sdg?.let {
                    Image(
                        painter = painterResource(
                            id = context.resources.getIdentifier(
                                it.imageName,
                                "drawable",
                                context.packageName
                            )
                        ),
                        contentDescription = it.name[localLanguageCode] ?: it.name["en"],
                        modifier = Modifier
                            .width(64.dp)
                            .height(64.dp)
                            .align(Alignment.CenterVertically)
                            .offset(y = (-16).dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // SDG Name
            sdg?.let {
                Text(
                    text = it.name[localLanguageCode] ?: it.name["en"] ?: "",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        //.fillMaxWidth()
                        .padding(top = 4.dp)
                        .align(Alignment.End)
                )
            }
        }
    }
}

