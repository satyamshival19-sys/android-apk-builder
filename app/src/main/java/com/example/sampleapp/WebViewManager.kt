package com.example.sampleapp

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
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
            settings.setSupportMultipleWindows(false)
            
            // 🔥 Hardware Acceleration Bypass & High Priority
            setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
            
            val safeName = scriptName.replace(" ", "_").replace(".", "_")
            loadDataWithBaseURL("http://127.0.0.1:8080/$safeName/", htmlCode, "text/html", "UTF-8", null)
            
            webChromeClient = WebChromeClient() 
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.let { injectAntiThrottleScript(it) }
                }

                // अगर OS ज़बरदस्ती WebView मारे, तो उसे रीस्टार्ट करो
                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    view?.let {
                        (it.parent as? ViewGroup)?.removeView(it)
                        it.destroy()
                    }
                    webViewMap.remove(scriptName)
                    getOrCreateWebView(context, scriptName)
                    return true
                }
            }
            
            // 🔥 HTML से Kotlin को जोड़ने वाला ब्रिज
            addJavascriptInterface(WebAppInterface(), "Android")
            
            isFocusable = true
            isFocusableInTouchMode = true
            resumeTimers()
        }

        webViewMap[scriptName] = newWebView
        return newWebView
    }

    private fun injectAntiThrottleScript(webView: WebView) {
        val jsCode = """
            (function() {
                // OS को धोखा देना (Visibility Override)
                Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                document.hasFocus = function() { return true; };

                // साइलेंट ऑडियो (WebView सोने न पाए)
                let silentAudio = document.getElementById('__silent_wake__');
                if (!silentAudio) {
                    silentAudio = document.createElement('audio');
                    silentAudio.id = '__silent_wake__';
                    silentAudio.loop = true;
                    silentAudio.src = 'data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=';
                    silentAudio.volume = 0.01;
                    document.body.appendChild(silentAudio);
                }
                setInterval(() => { if (silentAudio.paused) silentAudio.play().catch(e => {}); }, 3000);
            })();
        """.trimIndent()
        webView.evaluateJavascript(jsCode, null)
    }

    fun removeWebView(scriptName: String) {
        webViewMap[scriptName]?.let { webView ->
            val safeName = scriptName.replace(" ", "_").replace(".", "_")
            val originUrl = "http://127.0.0.1:8080/$safeName/"
            HtmlService.detachWebViewFromOverlay(webView)
            WebStorage.getInstance().deleteOrigin(originUrl) 
            webView.stopLoading()
            webView.loadUrl("about:blank") 
            webView.clearCache(true)
            webView.clearHistory()
            webView.removeJavascriptInterface("Android")
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
