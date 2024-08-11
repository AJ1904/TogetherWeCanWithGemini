package com.ajain.togetherwecanwithgemini.utils

import android.util.Log
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// Converts a Markdown string to HTML format
fun markdownToHtml(markdown: String): String {
    // Create a Markdown parser and renderer
    val parser = Parser.builder().build()
    val document = parser.parse(markdown)
    val renderer = HtmlRenderer.builder().build()

    // Render the parsed Markdown document to HTML
    var html = renderer.render(document)

    // Replace Markdown-style newlines with <br> tags for proper line breaks in HTML
    html = html.replace("\n", "<br>")

    return html
}

// Composable function to display HTML content in a WebView
@Composable
fun HtmlTextView(htmlText: String) {
    // Extract primary and on-primary colors from the MaterialTheme
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()

    // Create a styled HTML string with background and text colors
    val styledHtml = """
        <html>
            <body style="background-color:${Integer.toHexString(primaryColor).drop(2)};color:${Integer.toHexString(onPrimaryColor).drop(2)};">
                ${htmlText}
            </body>
        </html>
    """.trimIndent()
    
    // Log the generated HTML for debugging purposes
    Log.d("", styledHtml)

    // Create and configure a WebView to display the HTML content
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Ensure the WebView takes up the available space
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true // Enable JavaScript for dynamic content
                    domStorageEnabled = true // Enable DOM storage for storing data locally
                    setSupportZoom(false) // Disable zooming functionality
                }
                loadData(styledHtml, "text/html", "UTF-8") // Load the styled HTML content
            }
        },
        update = { webView ->
            // Update the WebView content if needed
            webView.loadData(styledHtml, "text/html", "UTF-8")
        }
    )
}
