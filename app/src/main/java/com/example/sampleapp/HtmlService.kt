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
import android.webkit.WebStorage
import java.io.File

class HtmlService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isUserSwiped = false
    private var server: LocalServer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try { startForeground(1, buildNotification()) } catch (e: Exception) {}

        if (server == null) {
            server = LocalServer(applicationContext, 8080)
            try { server?.start() } catch (e: Exception) { e.printStackTrace() }
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScriptApp::WakeLock")
            wakeLock?.acquire() 
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 🔥 FIX 4: OS ne kill kiya toh restart hone par scripts wapas load karo
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        for ((key, value) in prefs.all) {
            if (key.startsWith("run_") && value == true) {
                val savedScript = key.replace("run_", "")
                val file = File(filesDir, "scripts/$savedScript")
                if (file.exists()) {
                    WebViewManager.getOrCreateWebView(applicationContext, savedScript)
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
            .setContentTitle("बैकग्राउंड सर्विस एक्टिव")
            .setContentText("आपकी स्क्रिप्ट्स 24x7 चल रही हैं...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Ye sirf Recent Apps se swipe karne par chalega (Sab data clear)
        isUserSwiped = true
        WebViewManager.destroyAllWebViews()
        WebStorage.getInstance().deleteAllData()
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        stopSelf()
    }

    override fun onDestroy() {
        // 🔥 FIX 5: Microsecond (1000ms) Auto Restart
        if (!isUserSwiped) {
            val myIntent = Intent(applicationContext, HtmlService::class.java)
            val pendingIntent = PendingIntent.getService(this, 1, myIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pendingIntent)
            } else {
                alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, pendingIntent)
            }
        }
        server?.stop()
        server = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
