package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout

class DisplayActivity : Activity() {
    private var webView: WebView? = null
    private var container: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scriptName = intent.getStringExtra("SCRIPT_NAME") ?: return finish()

        webView = WebViewManager.getOrCreateWebView(this, scriptName)
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        container = FrameLayout(this)
        container?.addView(webView)
        setContentView(container)
    }

    override fun onPause() {
        super.onPause()
        // Background mein JS chalne ke liye pause nahi kiya
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            webView?.onResume()
            webView?.resumeTimers()
            webView?.requestLayout()
            webView?.invalidate()
        }, 100)
    }

    override fun onDestroy() {
        (webView?.parent as? ViewGroup)?.removeView(webView)
        super.onDestroy()
    }
}
