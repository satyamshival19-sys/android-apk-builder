package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout

class DisplayActivity : Activity() {
    private var webView: WebView? = null
    private var container: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val scriptName = intent.getStringExtra("SCRIPT_NAME") ?: return finish()
        webView = WebViewManager.getOrCreateWebView(this, scriptName)
        (webView?.parent as? ViewGroup)?.removeView(webView)
        container = FrameLayout(this)
        container?.addView(webView)
        setContentView(container)
    }

    override fun onPause() {
        super.onPause()
        // ऐप मिनिमाइज होते ही WebView को अदृश्य विंडो में भेजें
        webView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            HtmlService.attachWebViewToOverlay(it)
        }
    }

    override fun onResume() {
        super.onResume()
        // वापस आते ही WebView को स्क्रीन पर लाएं
        webView?.let {
            HtmlService.detachWebViewFromOverlay(it)
            (it.parent as? ViewGroup)?.removeView(it)
            container?.addView(it)
            Handler(Looper.getMainLooper()).postDelayed({
                it.onResume()
                it.resumeTimers()
                it.requestLayout()
                it.invalidate()
            }, 100)
        }
    }

    override fun onDestroy() {
        HtmlService.detachWebViewFromOverlay(webView ?: return)
        (webView?.parent as? ViewGroup)?.removeView(webView)
        super.onDestroy()
    }
}
