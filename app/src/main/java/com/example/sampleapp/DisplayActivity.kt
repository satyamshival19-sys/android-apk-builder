package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
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

        // WebViewManager से पहले से चालू WebView उठाओ (State Preservation)
        webView = WebViewManager.getOrCreateWebView(this, fileName)
        
        // अगर WebView पहले किसी और स्क्रीन पर थी, तो उसे उससे डिटैच करो
        (webView?.parent as? ViewGroup)?.removeView(webView)
        
        // स्क्रीन पर सेट करो
        setContentView(webView)
    }

    override fun onDestroy() {
        // स्क्रीन से हटाओ, लेकिन डिस्ट्रॉय मत करो (ताकि बैकग्राउंड में चलती रहे)
        (webView?.parent as? ViewGroup)?.removeView(webView)
        instance = null
        super.onDestroy()
    }
}
