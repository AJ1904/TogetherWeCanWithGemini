package com.ajain.togetherwecanwithgemini

import AccountScreen
import android.content.Context
import android.content.Intent
import android.graphics.Color.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.ajain.togetherwecanwithgemini.data.Activity
import com.ajain.togetherwecanwithgemini.viewmodels.MainViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState

@Composable
fun DiscoverScreen(viewModel: MainViewModel) {
    val activities = viewModel.activities.collectAsLazyPagingItems()
    val swipeRefreshState = remember { SwipeRefreshState(isRefreshing = false) }

    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = {
            viewModel.refreshActivities()
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .systemBarsPadding()
        ) {
            items(activities.itemCount) { index ->
                val activity = activities[index]
                activity?.let {
                    ActivityItem(activity = it, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            when {
                activities.loadState.refresh is LoadState.Loading -> {
                    item {
                        LoadingIndicator()
                    }
                }

                activities.loadState.append is LoadState.Loading -> {
                    item {
                        LoadingIndicator()
                    }
                }

                activities.loadState.refresh is LoadState.Error -> {
                    val e = activities.loadState.refresh as LoadState.Error
                    item {
                        Text(
                            "Error: ${e.error.localizedMessage}",
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                activities.loadState.append is LoadState.Error -> {
                    val e = activities.loadState.append as LoadState.Error
                    item {
                        Text(
                            "Error: ${e.error.localizedMessage}",
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
            item {Spacer(modifier = Modifier.height(24.dp))}
        }
    }
}

@Composable
fun ActivityItem(activity: Activity, viewModel: MainViewModel, showActions: Boolean = true) {
    var likeCount by remember { mutableLongStateOf(activity.likeCount ?: 0) }
    val context = LocalContext.current
    var showUserProfileDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = activity.goalTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = activity.imageUrl,
                contentDescription = activity.goalTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = activity.detail, style = MaterialTheme.typography.bodyMedium)
            // Spacer(modifier = Modifier.height(4.dp))
            if (showActions) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,

                    ) {

                    Text(
                        text = activity.userDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .clickable {
                                showUserProfileDialog = true
                            }
                    )
                    //Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (likeCount > 0) {
                            Text(text = "$likeCount")
                        }
                        IconButton(onClick = {
                            viewModel.likeActivity(activity.id)
                            likeCount += 1
                        }) {
                            Icon(
                                Icons.Filled.Favorite,
                                contentDescription = stringResource(R.string.like)
                            )
                        }
                    }
                    IconButton(onClick = {
                        shareActivity(context, activity)
                    }) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                    
                }
                if (showUserProfileDialog) {
                    UserProfileDialog(
                        userId = activity.userId,
                        onDismissRequest = { showUserProfileDialog = false },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun UserProfileDialog(
    userId: String,
    onDismissRequest: () -> Unit,
    viewModel: MainViewModel
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
        ) {
            AccountScreen(
                userId = userId,
                onLogoutClick = {}, // No-op for logout in other user's profile
                viewModel = viewModel
            )
        }
        }


}


fun shareActivity(context: Context, activity: Activity) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, activity.goalTitle)
        putExtra(
            Intent.EXTRA_TEXT, """
            ${activity.goalTitle}
            ${activity.detail}
            - ${activity.userDisplayName}
        """.trimIndent()
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}

