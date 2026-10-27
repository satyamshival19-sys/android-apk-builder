package com.example.sampleapp

import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

object WebViewManager {
    // 4 WebViews को मेमोरी में सेव रखने के लिए HashMap
    private val webViewMap = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        // अगर इस स्क्रिप्ट का WebView पहले से मेमोरी में है, तो वही दो (सेटिंग सुरक्षित रहेगी)
        if (webViewMap.containsKey(scriptName)) {
            val existingWv = webViewMap[scriptName]
            if (existingWv != null) {
                return existingWv
            }
        }

        // अगर नहीं है, तो नया बनाओ
        val file = File(context.filesDir, "scripts/$scriptName")
        // 🔥 FIX: स्पेशल कैरेक्टर (स्पेस/डॉट) वाली फाइल भी यहाँ से सही पढ़ी जाएगी
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val newWebView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            
            // 🔥 Local Server वाला Base URL (ड्रॉपडाउन के लिए)
            loadDataWithBaseURL("http://127.0.0.1:8080/", htmlCode, "text/html", "UTF-8", null)
            
            webChromeClient = WebChromeClient() // Dropdown के लिए
            webViewClient = WebViewClient()
            
            addJavascriptInterface(WebAppInterface(context), "Android")
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            resumeTimers()
        }

        webViewMap[scriptName] = newWebView
        return newWebView
    }

    // जब टॉगल OFF हो, तो उस विशेष स्क्रिप्ट का WebView डिस्ट्रॉय करें
    fun removeWebView(scriptName: String) {
        webViewMap[scriptName]?.let {
            it.removeJavascriptInterface("Android")
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        webViewMap.remove(scriptName)
    }

    // पूरे ऐप के सारे WebViews को डिस्ट्रॉय करें (Recent Apps से हटाने पर)
    fun destroyAllWebViews() {
        for (key in webViewMap.keys.toList()) {
            removeWebView(key)
        }
    }
}
