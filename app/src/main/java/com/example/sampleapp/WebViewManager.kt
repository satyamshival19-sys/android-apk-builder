package com.example.sampleapp

import android.content.Context
import android.graphics.PixelFormat
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

object WebViewManager {
    val activeWebViews = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        if (activeWebViews.containsKey(scriptName)) {
            return activeWebViews[scriptName]!!
        }
        
        val file = File(context.filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        // 🔥 यहाँ सीधे context (Activity) का इस्तेमाल किया गया है ताकि ड्रॉपडाउन चले
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true 
            settings.databaseEnabled = true 
            settings.mediaPlaybackRequiresUserGesture = false 
            settings.allowFileAccess = true
            settings.allowUniversalAccessFromFileURLs = true 
            settings.allowFileAccessFromFileURLs = true
            
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            
            loadDataWithBaseURL("https://www.binance.com", htmlCode, "text/html", "UTF-8", null)
            addJavascriptInterface(WebAppInterface(context), "Android")
            
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            
            resumeTimers()
        }
        
        activeWebViews[scriptName] = webView
        return webView
    }

    // 🔥 WebView को पैरेंट (Activity) से सुरक्षित हटाना
    fun detachFromParent(context: Context, webView: WebView) {
        try {
            val parent = webView.parent
            if (parent != null) {
                if (parent is ViewGroup) {
                    parent.removeView(webView)
                } else {
                    val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wm.removeView(webView)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun removeWebView(context: Context, scriptName: String) {
        activeWebViews[scriptName]?.let {
            it.removeJavascriptInterface("Android")
            detachFromParent(context, it)
            it.destroy()
        }
        activeWebViews.remove(scriptName)
    }
}
