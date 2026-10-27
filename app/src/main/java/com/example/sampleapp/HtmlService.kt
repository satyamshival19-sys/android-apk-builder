package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock

class HtmlService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isUserSwiped = false

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
                // 🔥 FIX: यहाँ से WebView बनाना हटा दिया गया है। 
                // अब WebView सिर्फ DisplayActivity बनाएगी (Activity Context के साथ)।
                // सर्विस सिर्फ बैकग्राउंड में जिंदा रहेगी।
            } else if (action == "STOP") {
                WebViewManager.removeWebView(this, scriptName)
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
        isUserSwiped = true
        for (key in WebViewManager.activeWebViews.keys.toList()) {
            WebViewManager.removeWebView(this, key)
        }
        stopSelf()
    }

    override fun onDestroy() {
        // 🔥 Immortal (अमर) फीचर: अगर OS मारे, तो 1 सेकंड में रीस्टार्ट हो
        if (!isUserSwiped) {
            val myIntent = Intent(applicationContext, HtmlService::class.java)
            val pendingIntent = PendingIntent.getService(this, 1, myIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pendingIntent)
        }
        
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
