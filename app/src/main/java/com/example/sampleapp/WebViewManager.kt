package com.example.sampleapp

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
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

        // 🔥 Context Fix: applicationContext का इस्तेमाल करें ताकि Activity डिस्ट्रॉय होने पर भी WebView जिंदा रहे
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

    // स्क्रीन पर दिखाने के लिए (Attach to Activity)
    fun attachToActivity(webView: WebView, container: ViewGroup) {
        if (webView.parent != null) {
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
        container.addView(webView)
    }

    // बैकग्राउंड में भेजने के लिए (Attach to Invisible Window)
    fun attachToWindowManager(context: Context, scriptName: String, webView: WebView) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) return
        
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
        try {
            wm.addView(webView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeWebView(scriptName: String) {
        activeWebViews[scriptName]?.let {
            it.removeJavascriptInterface("Android")
            if (it.parent != null) {
                (it.parent as? ViewGroup)?.removeView(it)
            } else {
                 try {
                     val wm = it.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                     wm.removeView(it)
                 } catch (e: Exception) {}
            }
            it.destroy()
        }
        activeWebViews.remove(scriptName)
    }
}
