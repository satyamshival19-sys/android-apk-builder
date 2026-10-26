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
    // 4 स्क्रिप्ट्स को मेमोरी में रखने के लिए HashMap
    val activeWebViews = HashMap<String, WebView>()

    fun getOrCreateWebView(context: Context, scriptName: String): WebView {
        if (activeWebViews.containsKey(scriptName)) {
            val wv = activeWebViews[scriptName]!!
            if (!wv.isDestroyed) return wv
        }
        
        val file = File(context.filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"
        val appContext = context.applicationContext

        val webView = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false // बैकग्राउंड साउंड के लिए
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

    // 🔥 क्रैश रोकने का सबसे बड़ा लॉजिक: View को पुराने पैरेंट से हटाना
    private fun detachFromParent(context: Context, webView: WebView) {
        try {
            if (webView.parent != null) {
                if (webView.parent is ViewGroup) {
                    (webView.parent as ViewGroup).removeView(webView)
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
                
                wm.addView(webView, params)
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
