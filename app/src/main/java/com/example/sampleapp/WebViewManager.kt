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
    private val windowManagerViews = HashMap<String, WindowManager>()

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

    fun attachToActivity(webView: WebView, container: ViewGroup) {
        Handler(Looper.getMainLooper()).post {
            try {
                if (webView.parent != null) {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                }
                container.addView(webView)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun attachToWindowManager(context: Context, scriptName: String, webView: WebView) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) return
        
        Handler(Looper.getMainLooper()).post {
            try {
                if (webView.parent != null) {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                }
                
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
                    windowManagerViews[scriptName] = wm
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun removeWebView(scriptName: String) {
        Handler(Looper.getMainLooper()).post {
            activeWebViews[scriptName]?.let {
                it.removeJavascriptInterface("Android")
                windowManagerViews[scriptName]?.let { wm ->
                    try { wm.removeView(it) } catch (e: Exception) {}
                }
                if (it.parent != null) {
                    (it.parent as? ViewGroup)?.removeView(it)
                }
                it.destroy()
            }
            activeWebViews.remove(scriptName)
            windowManagerViews.remove(scriptName)
        }
    }
}
