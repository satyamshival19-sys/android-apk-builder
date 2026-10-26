package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView

class DisplayActivity : Activity() {
    
    companion object {
        var instance: DisplayActivity? = null
    }

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        val fileName = intent.getStringExtra("SCRIPT_NAME")
        if (fileName == null) {
            finish()
            return
        }

        webView = WebViewManager.getOrCreateWebView(this, fileName)
        
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        // 🔥 सबसे जरुरी फिक्स: WebView को Activity का Window दें ताकि ड्रॉपडाउन खुले
        webView?.webChromeClient = WebChromeClient()
        webView?.isFocusable = true
        webView?.isFocusableInTouchMode = true
        webView?.requestFocus()
        
        setContentView(webView)
    }

    override fun onResume() {
        super.onResume()
        // वापस आने पर ब्लैक स्क्रीन रोकने के लिए
        Handler(Looper.getMainLooper()).postDelayed({
            webView?.onResume()
            webView?.resumeTimers()
            webView?.requestLayout()
            webView?.invalidate()
        }, 100)
    }

    override fun onDestroy() {
        (webView?.parent as? ViewGroup)?.removeView(webView)
        instance = null
        super.onDestroy()
    }
}
