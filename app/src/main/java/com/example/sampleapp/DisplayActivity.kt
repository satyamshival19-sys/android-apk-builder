package com.example.sampleapp

import android.app.Activity
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout

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

        webView = WebViewManager.getOrCreateWebView(this, scriptName!!)
        
        // WebView को WindowManager से हटाकर स्क्रीन पर लगाएं
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        val layout = FrameLayout(this)
        layout.addView(webView)
        setContentView(layout)
    }

    override fun onDestroy() {
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        // 🔥 बाहर निकलने पर WebView को वापस अदृश्य विंडो में डालें
        if (scriptName != null && WebViewManager.activeWebViews.containsKey(scriptName)) {
            if (android.provider.Settings.canDrawOverlays(this)) {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val params = WindowManager.LayoutParams(
                    0, 0, 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                )
                try { wm.addView(webView, params) } catch (e: Exception) {}
            }
        }
        instance = null
        super.onDestroy()
    }
}
