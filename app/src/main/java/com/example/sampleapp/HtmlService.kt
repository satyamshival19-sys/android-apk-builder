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
            .setContentText("आपकी स्क्रिप्ट्स 24x7 चल रही हैं...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isUserSwiped = true
        
        // 🔥 RESET FIX: ऐप को Recent Apps से हटाने पर सारा डेटा और सेटिंग्स रीसेट
        WebViewManager.destroyAllWebViews()
        WebStorage.getInstance().deleteAllData()
        
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        
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
        
        server?.stop()
        server = null

        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
