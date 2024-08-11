package com.ajain.togetherwecanwithgemini

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ajain.togetherwecanwithgemini.data.Detail
import com.ajain.togetherwecanwithgemini.utils.HtmlTextView
import com.ajain.togetherwecanwithgemini.utils.markdownToHtml
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Carousel(
    details: List<Detail>,
    modifier: Modifier = Modifier,
    localLanguageCode: String = "en"
) {
    val pagerState = rememberPagerState(pageCount = { details.size })
    val coroutineScope = rememberCoroutineScope() // Define the coroutine scope

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxHeight()
        ) { page ->
            val detail = details[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp) // Increase the padding to make space for the arrows
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary // Replace with your desired color
                ),

            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    LazyColumn {
                        item {
                            Text(
                                text = detail.title[localLanguageCode] ?: detail.title["en"] ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
//                            Text(
//                                //text = markdownToHtml(detail.body[localLanguageCode] ?: detail.body["en"] ?: ""),
//                                text = detail.body[localLanguageCode] ?: detail.body["en"] ?: "",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onPrimary
//                            )
                           // val htmlContent = markdownToHtml(detail.body[localLanguageCode] ?: detail.body["en"] ?: "")

                            //HtmlTextView(htmlContent)
                            val markdownContent = detail.body[localLanguageCode] ?: detail.body["en"] ?: ""
                            val htmlContent = markdownToHtml(markdownContent)

                            HtmlTextView(htmlContent)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Left arrow
        if (pagerState.currentPage > 0) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 0.dp, top = 16.dp, bottom = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(painter = painterResource(id = R.drawable.baseline_keyboard_arrow_left_24),
                    contentDescription = stringResource(
                    R.string.previous
                )
                            ,tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Right arrow
        if (pagerState.currentPage < pagerState.pageCount - 1) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 0.dp, top = 16.dp, bottom = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24), contentDescription = stringResource(
                    R.string.next
                ),tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}