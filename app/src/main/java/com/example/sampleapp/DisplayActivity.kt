package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

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

        // WebView हमेशा Activity के कॉन्टेक्स्ट से बनेगा (ड्रॉपडाउन के लिए)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true // Settings सेव रखने के लिए
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            
            // 🔥 लोकल सर्वर से लोड करें (ड्रॉपडाउन फिक्स)
            loadUrl("http://127.0.0.1:8080/$fileName")
            
            webChromeClient = WebChromeClient() 
            webViewClient = WebViewClient()
            
            addJavascriptInterface(WebAppInterface(this@DisplayActivity), "Android")
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            resumeTimers()
        }
        
        setContentView(webView)
    }

    // ऐप मिनिमाइज़ होने पर सिर्फ Pause होगा, डिस्ट्रॉय नहीं
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
        webView?.destroy()
        webView = null
        instance = null
        super.onDestroy()
    }
}
