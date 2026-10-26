package com.example.sampleapp

import android.content.Context
import android.webkit.WebView
import java.io.File

object WebViewManager {
    // यहाँ हम सभी चालू WebViews को सेव रखेंगे
    val activeWebViews = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        if (activeWebViews.containsKey(scriptName)) {
            return activeWebViews[scriptName]!!
        }
        
        val file = File(context.filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = true
            settings.allowUniversalAccessFromFileURLs = true // CORS Bypass
            settings.allowFileAccessFromFileURLs = true
            
            // Internet aur CORS ke liye base URL
            loadDataWithBaseURL("https://www.binance.com", htmlCode, "text/html", "UTF-8", null)
            addJavascriptInterface(WebAppInterface(context), "Android")
            resumeTimers()
        }
        
        activeWebViews[scriptName] = webView
        return webView
    }

    fun removeWebView(scriptName: String) {
        activeWebViews[scriptName]?.destroy()
        activeWebViews.remove(scriptName)
    }
}
