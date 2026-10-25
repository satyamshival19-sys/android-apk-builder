package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import java.io.File

class HtmlService : Service() {
    private var webView: WebView? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(1, buildNotification())
        } catch (e: Exception) {
            // अगर नोटिफिकेशन परमिशन नहीं है, तो भी क्रैश न हो
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scriptName = intent?.getStringExtra("SCRIPT_NAME") ?: return START_NOT_STICKY
        val file = File(filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScriptApp::WakeLock")
            wakeLock?.acquire() 
        }

        // पुराना WebView डिस्ट्रॉय करें (अगर चल रहा है)
        webView?.destroy()

        // नया WebView बनाएँ (WindowManager की जरूरत नहीं है)
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(WebAppInterface(this@HtmlService), "Android")
            loadDataWithBaseURL(null, htmlCode, "text/html", "UTF-8", null)
            resumeTimers() // बैकग्राउंड में टाइमर चालू रखने के लिए
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "html_alert_channel", "Script Alerts", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "html_alert_channel")
            .setContentTitle("स्क्रिप्ट रनर")
            .setContentText("बैकग्राउंड में चल रहा है...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        webView?.destroy()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }
}
