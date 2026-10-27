package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout

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

        // WebViewManager से वही WebView उठाओ जो मेमोरी में सेव है
        webView = WebViewManager.getOrCreateWebView(this, fileName)
        
        // अगर WebView पहले किसी स्क्रीन पर थी, तो उसे हटाओ
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        // एक FrameLayout बनाकर उसमें WebView डालें
        val layout = FrameLayout(this)
        layout.addView(webView)
        setContentView(layout)
    }

    override fun onPause() {
        super.onPause()
        webView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        webView?.resumeTimers()
    }

    override fun onDestroy() {
        // 🔥 FIX: यहाँ WebView को डिस्ट्रॉय नहीं करेंगे, सिर्फ स्क्रीन से हटाएंगे 
        // ताकि आपकी सेटिंग (Alert Armed) वैसी ही रहे।
        (webView?.parent as? ViewGroup)?.removeView(webView)
        instance = null
        super.onDestroy()
    }
}
