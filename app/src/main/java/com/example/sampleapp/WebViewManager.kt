package com.example.sampleapp

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.WindowManager
import android.webkit.WebView
import java.io.File

object WebViewManager {
    val activeWebViews = HashMap<String, WebView>()
    private val windowManagerViews = HashMap<String, WindowManager>()

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
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowFileAccessFromFileURLs = true
            
            loadDataWithBaseURL("https://www.binance.com", htmlCode, "text/html", "UTF-8", null)
            addJavascriptInterface(WebAppInterface(context), "Android")
            resumeTimers()
        }
        
        // 🔥 अदृश्य विंडो में डालना (Immortal Feature)
        if (Settings.canDrawOverlays(context)) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                0, 0, 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            try {
                wm.addView(webView, params)
                windowManagerViews[scriptName] = wm
            } catch (e: Exception) {}
        }
        
        activeWebViews[scriptName] = webView
        return webView
    }

    fun removeWebView(scriptName: String) {
        activeWebViews[scriptName]?.let {
            it.removeJavascriptInterface("Android")
            windowManagerViews[scriptName]?.removeView(it)
            windowManagerViews.remove(scriptName)
            it.destroy()
        }
        activeWebViews.remove(scriptName)
    }
}
