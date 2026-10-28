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
import android.widget.Toast
import java.io.File

class HtmlService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isUserStopped = false 
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
        // 🔥 Notification STOP button logic
        if (intent?.action == "STOP_SERVICE") {
            isUserStopped = true
            WebViewManager.destroyAllWebViews()
            WebStorage.getInstance().deleteAllData()
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().clear().commit()
            stopSelf()
            return START_NOT_STICKY
        }

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
        // 🔥 Notification STOP Button
        val stopIntent = Intent(this, HtmlService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "html_alert_channel")
            .setContentTitle("App Running")
            .setContentText("Scripts are active in background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "STOP", stopPendingIntent) 
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 🔥 FIX: App background se swipe hone par band NAHI hogi
        Toast.makeText(applicationContext, "App background mein chalti rahegi...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        // Auto-Restart (1 sec) agar user ne STOP nahi kiya
        if (!isUserStopped) {
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
