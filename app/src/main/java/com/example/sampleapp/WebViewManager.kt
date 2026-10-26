package com.example.sampleapp

import android.content.Context
import android.webkit.WebView
import java.io.File

object WebViewManager {
    val activeWebViews = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        if (activeWebViews.containsKey(scriptName)) {
            return activeWebViews[scriptName]!!
        }
        
        val file = File(context.filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val appContext = context.applicationContext

        val webView = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true 
            settings.databaseEnabled = true 
            settings.mediaPlaybackRequiresUserGesture = false 
            settings.allowFileAccess = true
            settings.allowUniversalAccessFromFileURLs = true 
            settings.allowFileAccessFromFileURLs = true
            
            loadDataWithBaseURL("https://www.binance.com", htmlCode, "text/html", "UTF-8", null)
            addJavascriptInterface(WebAppInterface(appContext), "Android")
            
            resumeTimers()
        }
        
        activeWebViews[scriptName] = webView
        return webView
    }

    fun removeWebView(scriptName: String) {
        activeWebViews[scriptName]?.let {
            it.removeJavascriptInterface("Android")
            it.destroy()
        }
        activeWebViews.remove(scriptName)
    }
}
