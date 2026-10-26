package com.example.sampleapp

import android.content.Context
import android.webkit.WebView
import java.io.File

object WebViewManager {
    // सभी चालू WebViews को नाम के साथ सेव रखेंगे
    val activeWebViews = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        // अगर WebView पहले से चल रही है, तो वही रिटर्न करो (सेटिंग सुरक्षित रहेगी)
        if (activeWebViews.containsKey(scriptName)) {
            return activeWebViews[scriptName]!!
        }
        
        val file = File(context.filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        // नई WebView बनाओ (Exact On-Screen Condition के साथ)
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false // साउंड बैकग्राउंड में चलेगा
            settings.allowFileAccess = true
            settings.allowUniversalAccessFromFileURLs = true // CORS बायपास
            settings.allowFileAccessFromFileURLs = true
            
            // इंटरनेट और API के लिए बेस URL
            loadDataWithBaseURL("https://www.binance.com", htmlCode, "text/html", "UTF-8", null)
            
            addJavascriptInterface(WebAppInterface(context), "Android")
            
            // टाइमर को रुकने से रोकें
            resumeTimers()
        }
        
        activeWebViews[scriptName] = webView
        return webView
    }

    // जब टॉगल OFF हो या ऐप बंद हो, तो WebView को पूरी तरह डिस्ट्रॉय करें
    fun removeWebView(scriptName: String) {
        activeWebViews[scriptName]?.let {
            it.removeJavascriptInterface("Android")
            it.destroy()
        }
        activeWebViews.remove(scriptName)
    }
}
