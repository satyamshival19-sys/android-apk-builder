package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager
import android.webkit.WebView
import androidx.core.app.NotificationCompat

class HtmlService : Service() {
    private lateinit var webView: WebView
    private lateinit var windowManager: WindowManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(WebAppInterface(this@HtmlService), "Android")
            loadUrl("file:///android_asset/index.html")
        }

        val params = WindowManager.LayoutParams(
            0, 0,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.RGBA_8888
        )
        windowManager.addView(webView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "html_alert_channel", "HTML अलर्ट", NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "html_alert_channel")
            .setContentTitle("HTML अलर्ट सर्विस")
            .setContentText("बैकग्राउंड में चल रही है...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        windowManager.removeViewImmediate(webView)
        super.onDestroy()
    }
}
