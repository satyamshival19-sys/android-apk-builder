package com.example.sampleapp

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

class DisplayActivity : Activity() {
    
    companion object {
        var instance: DisplayActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)
        
        instance = this

        val fileName = intent.getStringExtra("SCRIPT_NAME") ?: return
        val file = File(filesDir, "scripts/$fileName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.loadsImagesAutomatically = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        
        // 👇 यहाँ हमने Base URL जोड़ा है ताकि API से डेटा आ सके
        webView.loadDataWithBaseURL("https://www.binance.com", htmlCode, "text/html", "UTF-8", null)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
