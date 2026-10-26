package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.webkit.WebView

class DisplayActivity : Activity() {
    companion object { var instance: DisplayActivity? = null }
    private var webView: WebView? = null
    private var scriptName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        scriptName = intent.getStringExtra("SCRIPT_NAME")
        
        if (scriptName == null) {
            finish()
            return
        }

        // WebView प्राप्त करें
        webView = WebViewManager.getOrCreateWebView(this, scriptName!!)
        
        val layout = FrameLayout(this)
        setContentView(layout)
        
        // स्क्रीन पर अटैच करें
        WebViewManager.attachToActivity(webView!!, layout)
    }

    override fun onDestroy() {
        // स्क्रीन बंद होने पर, वापस बैकग्राउंड में भेजें (अगर यह अभी भी चालू है)
        if (scriptName != null && webView != null && WebViewManager.activeWebViews.containsKey(scriptName)) {
            WebViewManager.attachToWindowManager(this, scriptName!!, webView!!)
        }
        instance = null
        super.onDestroy()
    }
}
