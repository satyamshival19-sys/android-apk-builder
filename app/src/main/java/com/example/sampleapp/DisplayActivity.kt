package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.webkit.WebView

class DisplayActivity : Activity() {
    companion object { var instance: DisplayActivity? = null }
    private var webView: WebView? = null
    private var scriptName: String? = null
    private var container: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        scriptName = intent.getStringExtra("SCRIPT_NAME")
        
        if (scriptName == null) {
            finish()
            return
        }

        webView = WebViewManager.getOrCreateWebView(this, scriptName!!)
        
        container = FrameLayout(this)
        setContentView(container)
        
        WebViewManager.attachToActivity(this, webView!!, container!!)
    }

    override fun onResume() {
        super.onResume()
        // 🔥 ब्लैक स्क्रीन फिक्स: जब वापस आएं, तो व्यू को फिर से अटैच करें
        if (webView != null && container != null) {
            WebViewManager.attachToActivity(this, webView!!, container!!)
        }
    }

    override fun onDestroy() {
        // 🔥 जब मिनिमाइज़ हो, तो बैकग्राउंड में भेजें
        if (scriptName != null && webView != null && WebViewManager.activeWebViews.containsKey(scriptName)) {
            WebViewManager.attachToWindowManager(this, scriptName!!, webView!!)
        }
        instance = null
        super.onDestroy()
    }
}
