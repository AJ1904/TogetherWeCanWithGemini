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
fun markdownToHtml(markdown: String): String {
    val parser = Parser.builder().build()
    val document = parser.parse(markdown)
    val renderer = HtmlRenderer.builder().build()
    var html = renderer.render(document)

  //  Log.d("22",html)
    // Replace Markdown-style newlines with <br> tags
    html = html.replace("\n", "<br>")
    //Log.d("25",html)

    return html
}


//fun markdownToHtml(markdown: String): String {
//    val parser = Parser.builder().build()
//    val document = parser.parse(markdown)
//    val renderer = HtmlRenderer.builder().build()
//    val html = renderer.render(document).replace("\n", "<br>")
//    return html
//    //return renderer.render(document)
//}

@Composable
fun HtmlTextView(htmlText: String) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()

    val styledHtml = """
        <html>
            <body style="background-color:${Integer.toHexString(primaryColor).drop(2)};color:${Integer.toHexString(onPrimaryColor).drop(2)};">
                ${htmlText}
            </body>
        </html>
    """.trimIndent()
Log.d("",styledHtml)
    AndroidView(
        modifier = Modifier.fillMaxSize(), // Ensure it takes up space
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setSupportZoom(false)
                }
                loadData(styledHtml, "text/html", "UTF-8")
            }
        },
        update = { webView ->
            webView.loadData(styledHtml, "text/html", "UTF-8")
        }
    )
}
