package com.example.sampleapp

import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

object WebViewManager {
    // WebView को मेमोरी में सेव रखेंगे ताकि सेटिंग रीसेट न हो
    var myWebView: WebView? = null
    var currentScriptName: String? = null

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        // अगर वही स्क्रिप्ट पहले से चल रही है, तो उसी WebView को लौटाओ
        if (myWebView != null && currentScriptName == scriptName) {
            return myWebView!!
        }

        // अगर नई स्क्रिप्ट है, तो पुराना डिस्ट्रॉय करो
        destroyWebView()

        val file = File(context.filesDir, "scripts/$scriptName")
        // 🔥 FIX: स्पेशल कैरेक्टर वाली फाइल भी यहाँ से सही पढ़ी जाएगी
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        myWebView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            
            // 🔥 FIX: URL में फाइल का नाम भेजने के बजाय सीधे कोड लोड करें
            loadDataWithBaseURL("http://127.0.0.1:8080/", htmlCode, "text/html", "UTF-8", null)
            
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            
            addJavascriptInterface(WebAppInterface(context), "Android")
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            resumeTimers()
        }

        currentScriptName = scriptName
        return myWebView!!
    }

    fun destroyWebView() {
        myWebView?.removeJavascriptInterface("Android")
        myWebView?.destroy()
        myWebView = null
        currentScriptName = null
    }
}
