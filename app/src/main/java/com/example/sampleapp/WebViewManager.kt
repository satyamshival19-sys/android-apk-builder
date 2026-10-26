package com.example.sampleapp

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import java.io.File

object WebViewManager {
    val activeWebViews = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        // 🔥 Fix: यहाँ से isDestroyed वाला चेक हटा दिया गया है ताकि बिल्ड फेल न हो
        if (activeWebViews.containsKey(scriptName)) {
            return activeWebViews[scriptName]!!
        }
        
        val file = File(context.filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"
        val appContext = context.applicationContext

        val webView = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
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

    private fun detachFromParent(context: Context, webView: WebView) {
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

    fun attachToActivity(context: Context, webView: WebView, container: ViewGroup) {
        Handler(Looper.getMainLooper()).post {
            try {
                detachFromParent(context, webView)
                container.addView(webView)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun attachToWindowManager(context: Context, scriptName: String, webView: WebView) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) return
        
        Handler(Looper.getMainLooper()).post {
            try {
                detachFromParent(appContext, webView)
                
                val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val params = WindowManager.LayoutParams(
                    0, 0, 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                
                if (webView.parent == null) {
                    wm.addView(webView, params)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun removeWebView(context: Context, scriptName: String) {
        Handler(Looper.getMainLooper()).post {
            activeWebViews[scriptName]?.let {
                it.removeJavascriptInterface("Android")
                detachFromParent(context, it)
                it.destroy()
            }
            activeWebViews.remove(scriptName)
        }
    }
}
