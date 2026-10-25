package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

class DisplayActivity : Activity() {
    
    // यह लाइन हमें इस स्क्रीन को बाहर से बंद करने की शक्ति देगी
    companion object {
        var instance: DisplayActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)
        
        instance = this // स्क्रीन खुलते ही इसे रजिस्टर करें

        val fileName = intent.getStringExtra("SCRIPT_NAME") ?: return
        val file = File(filesDir, "scripts/$fileName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        webView.loadDataWithBaseURL(null, htmlCode, "text/html", "UTF-8", null)
    }

    override fun onDestroy() {
        instance = null // स्क्रीन बंद होते ही रजिस्टर से हटा दें
        super.onDestroy()
    }
}
