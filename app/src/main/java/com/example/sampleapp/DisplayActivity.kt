package com.example.sampleapp

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class DisplayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        val fileName = intent.getStringExtra("SCRIPT_NAME") ?: return
        val file = File(filesDir, "scripts/$fileName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(this), "Android")
        
        // HTML स्क्रीन पर लोड करें
        webView.loadDataWithBaseURL(null, htmlCode, "text/html", "UTF-8", null)
    }
}
