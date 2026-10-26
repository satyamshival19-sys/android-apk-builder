package com.example.sampleapp

import android.app.Activity
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout

class DisplayActivity : Activity() {
    
    companion object {
        var instance: DisplayActivity? = null
    }

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

        // Activity Context पास कर रहे हैं ताकि ड्रॉपडाउन चले
        webView = WebViewManager.getOrCreateWebView(this, scriptName!!)
        
        container = FrameLayout(this)
        setContentView(container)
        
        (webView?.parent as? ViewGroup)?.removeView(webView)
        container?.addView(webView)
    }

    override fun onResume() {
        super.onResume()
        // 🔥 BLACK SCREEN FIX: वापस आने पर WebView को रिफ्रेश करें
        Handler(Looper.getMainLooper()).postDelayed({
            webView?.onResume()
            webView?.resumeTimers()
            webView?.requestLayout()
            webView?.invalidate()
        }, 100)
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onDestroy() {
        // 🔥 जब मिनिमाइज़ हो, तो WebView को बैकग्राउंड (WindowManager) में भेजें
        if (scriptName != null && webView != null && WebViewManager.activeWebViews.containsKey(scriptName)) {
            if (Settings.canDrawOverlays(this)) {
                try {
                    (webView?.parent as? ViewGroup)?.removeView(webView)
                    
                    val wm = applicationContext.getSystemService(WINDOW_SERVICE) as WindowManager
                    val params = WindowManager.LayoutParams(
                        1, 1,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                    )
                    params.x = -100
                    params.y = -100
                    wm.addView(webView, params)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        instance = null
        super.onDestroy()
    }
}
