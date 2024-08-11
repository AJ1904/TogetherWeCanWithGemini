package com.ajain.togetherwecanwithgemini

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ajain.togetherwecanwithgemini.utils.markdownToHtml

// Convert Markdown content to HTML format and create a WebView to display the HTML content
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MarkdownWebView(markdown: String) {
    val context = LocalContext.current
    val htmlContent = markdownToHtml(markdown)

    AndroidView(factory = {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        }
    })
}