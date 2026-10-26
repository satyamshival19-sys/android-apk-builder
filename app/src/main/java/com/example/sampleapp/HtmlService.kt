package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class HtmlService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(1, buildNotification())
        } catch (e: Exception) {}

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScriptApp::WakeLock")
            wakeLock?.acquire() 
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val scriptName = intent?.getStringExtra("SCRIPT_NAME")

        if (scriptName != null) {
            if (action == "START") {
                val webView = WebViewManager.getOrCreateWebView(this, scriptName)
                webView.resumeTimers() 
            } else if (action == "STOP") {
                WebViewManager.removeWebView(scriptName)
                if (WebViewManager.activeWebViews.isEmpty()) {
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "html_alert_channel", "Script Alerts", NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "html_alert_channel")
            .setContentTitle("बैकग्राउंड सर्विस एक्टिव")
            .setContentText("आपकी स्क्रिप्ट्स बिना रुके चल रही हैं...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // पॉइंट 4 और 6: जब ऐप को बैकग्राउंड (Recent Apps) से हटाया जाए
        // तो सारी स्क्रिप्ट बंद (डिस्ट्रॉय) हो जाएँ और टॉगल रीसेट हो जाएँ।
        for (key in WebViewManager.activeWebViews.keys.toList()) {
            WebViewManager.removeWebView(key)
        }
        stopSelf()
    }

    override fun onDestroy() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
