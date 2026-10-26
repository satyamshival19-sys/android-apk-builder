package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock
import androidx.core.app.NotificationCompat

class HtmlService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try { startForeground(1, buildNotification()) } catch (e: Exception) {}

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
                if (WebViewManager.activeWebViews.isEmpty()) stopSelf()
            }
        } else {
            // 🔥 Immortal Recovery: अगर OS ने ऐप को मारा था और रीस्टार्ट हुआ है
            val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val allKeys = prefs.all
            for ((key, value) in allKeys) {
                if (key.startsWith("run_") && value == true) {
                    val savedScript = key.replace("run_", "")
                    val webView = WebViewManager.getOrCreateWebView(this, savedScript)
                    webView.resumeTimers()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("html_alert_channel", "Script Alerts", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "html_alert_channel")
            .setContentTitle("सिस्टम 24x7 एक्टिव")
            .setContentText("आपकी स्क्रिप्ट्स बिना रुके चल रही हैं...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 🔥 पॉइंट 4, 6: जब यूजर ऐप को स्वाइप करके हटाए, तो सारी स्क्रिप्ट बंद और टॉगल रीसेट
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
        for (key in WebViewManager.activeWebViews.keys.toList()) {
            WebViewManager.removeWebView(key)
        }
        stopSelf()
    }

    override fun onDestroy() {
        // 🔥 Immortal Feature: अगर OS जबरदस्ती मारे, तो 1 सेकंड बाद खुद को रीस्टार्ट करेगा
        val myIntent = Intent(applicationContext, HtmlService::class.java)
        val pendingIntent = PendingIntent.getService(this, 1, myIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pendingIntent)

        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
