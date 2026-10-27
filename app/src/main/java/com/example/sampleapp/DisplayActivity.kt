package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout

class DisplayActivity : Activity() {
    
    companion object {
        var instance: DisplayActivity? = null
    }

    private var webView: WebView? = null
    private var container: FrameLayout? = null
    private var scriptName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        scriptName = intent.getStringExtra("SCRIPT_NAME")
        
        if (scriptName == null) {
            finish()
            return
        }

        // मेमोरी से इस स्क्रिप्ट का WebView उठाओ
        webView = WebViewManager.getOrCreateWebView(this, scriptName!!)
        
        // अगर WebView पहले किसी स्क्रीन पर थी, तो उसे डिटैच करो
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        // नया कंटेनर बनाकर उसमें डालो
        container = FrameLayout(this)
        container?.addView(webView)
        setContentView(container)
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        // 🔥 BLACK SCREEN FIX: वापस आने पर व्यू को रिफ्रेश करें
        Handler(Looper.getMainLooper()).postDelayed({
            webView?.onResume()
            webView?.resumeTimers()
            webView?.requestLayout()
            webView?.invalidate()
        }, 100)
    }

    override fun onDestroy() {
        // 🔥 FIX: यहाँ WebView डिस्ट्रॉय नहीं होगा, वह मेमोरी में सेव रहेगा
        (webView?.parent as? ViewGroup)?.removeView(webView)
        instance = null
        super.onDestroy()
    }
}
