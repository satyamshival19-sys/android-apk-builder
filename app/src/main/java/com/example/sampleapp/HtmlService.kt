package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import java.io.File

class HtmlService : Service() {
    private var webView: WebView? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentScriptName: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            startForeground(1, buildNotification())
        } catch (e: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentScriptName = intent?.getStringExtra("SCRIPT_NAME")
        val scriptName = currentScriptName ?: return START_STICKY
        
        val file = File(filesDir, "scripts/$scriptName")
        val htmlCode = if (file.exists()) file.readText() else "<h1>File Not Found</h1>"

        // CPU को जगाए रखें
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScriptApp::WakeLock")
            wakeLock?.acquire() 
        }

        webView?.destroy()
        webView = WebView(this).apply {
            // HTML को पूरी आज़ादी
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false 
            webViewClient = WebViewClient()
            
            // बिना किसी ब्रिज के सीधा लोड करें
            loadDataWithBaseURL("https://www.binance.com", htmlCode, "text/html", "UTF-8", null)
            
            // टाइमर चालू रखें
            resumeTimers() 
        }

        return START_STICKY
    }

    // ऐप बंद होने पर खुद चालू हो जाए
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (currentScriptName != null) {
            val restartServiceIntent = Intent(applicationContext, HtmlService::class.java)
            restartServiceIntent.putExtra("SCRIPT_NAME", currentScriptName)
            val restartServicePendingIntent = PendingIntent.getService(
                this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                System.currentTimeMillis() + 1000, 
                restartServicePendingIntent
            )
        }
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
            .setContentText("आपकी स्क्रिप्ट चल रही है...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        webView?.destroy()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
