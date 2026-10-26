package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        
        // UI थ्रेड को सेटल होने का समय दें
        Handler(Looper.getMainLooper()).postDelayed({
            if (webView != null && container != null) {
                WebViewManager.attachToActivity(this, webView!!, container!!, scriptName!!)
            }
        }, 100)
    }

    override fun onResume() {
        super.onResume()
        if (webView != null && container != null && webView?.parent == null) {
            Handler(Looper.getMainLooper()).postDelayed({
                WebViewManager.attachToActivity(this, webView!!, container!!, scriptName!!)
            }, 100)
        }
    }

    override fun onDestroy() {
        // मिनिमाइज़ होने पर बैकग्राउंड (WindowManager) में भेजें
        if (scriptName != null && webView != null && WebViewManager.currentScriptName == scriptName) {
            WebViewManager.attachToWindowManager(this, scriptName!!, webView!!)
        }
        instance = null
        super.onDestroy()
    }
}
