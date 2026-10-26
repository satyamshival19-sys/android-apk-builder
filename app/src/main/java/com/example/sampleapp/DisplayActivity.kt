package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast

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

        // WebViewManager se pehle se chalu WebView uthao (State Preservation)
        webView = WebViewManager.getOrCreateWebView(this, fileName)
        
        // Agar WebView kisi aur screen par attached hai, toh use hatao
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        // Screen par set karo
        setContentView(webView)
    }

    override fun onDestroy() {
        // Screen se hatao, par destroy mat karo (taaki background me chalta rahe)
        (webView?.parent as? ViewGroup)?.removeView(webView)
        instance = null
        super.onDestroy()
    }
}
