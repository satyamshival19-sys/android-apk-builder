package com.example.sampleapp

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

object WebViewManager {
    private val webViewMap = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        if (webViewMap.containsKey(scriptName)) {
            webViewMap[scriptName]?.let { return it }
        }

        val file = File(context.filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val appContext = context.applicationContext
        
        val newWebView = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            
            // Unique URL for Isolated Local Storage
            val safeName = scriptName.replace(" ", "_").replace(".", "_")
            loadDataWithBaseURL("http://127.0.0.1:8080/$safeName/", htmlCode, "text/html", "UTF-8", null)
            
            webChromeClient = WebChromeClient() 
            webViewClient = WebViewClient()
            
            addJavascriptInterface(WebAppInterface(appContext), "Android")
            isFocusable = true
            isFocusableInTouchMode = true
            resumeTimers()
        }

        webViewMap[scriptName] = newWebView
        return newWebView
    }

    fun removeWebView(scriptName: String) {
        webViewMap[scriptName]?.let { webView ->
            val safeName = scriptName.replace(" ", "_").replace(".", "_")
            val originUrl = "http://127.0.0.1:8080/$safeName/"
            
            // 1. Clear specific storage
            WebStorage.getInstance().deleteOrigin(originUrl) 
            
            // 2. Kill JS timers (Stop ghost alerts)
            webView.stopLoading()
            webView.loadUrl("about:blank") 
            webView.clearCache(true)
            webView.clearHistory()
            webView.removeJavascriptInterface("Android")
            
            // 3. Destroy View
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        webViewMap.remove(scriptName)
    }

    fun destroyAllWebViews() {
        for (key in webViewMap.keys.toList()) {
            removeWebView(key)
        }
    }
}
