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
                // WebViewManager se WebView banao ya uthao
                val webView = WebViewManager.getOrCreateWebView(this, scriptName)
                webView.resumeTimers() // Timers ko zinda rakho
            } else if (action == "STOP") {
                // Sirf is specific WebView ko destroy karo
                WebViewManager.removeWebView(scriptName)
                
                // Agar koi aur WebView active nahi hai, toh service band kar do
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
            .setContentText("आपकी स्क्रिप्ट्स चल रही हैं...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        // Service band hone par saari WebViews destroy karo
        for (key in WebViewManager.activeWebViews.keys.toList()) {
            WebViewManager.removeWebView(key)
        }
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
