package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.SystemClock
import android.widget.Toast
import java.io.File

class HtmlService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var isUserStopped = false 
    private var server: LocalServer? = null

    companion object {
        private var windowManager: WindowManager? = null
        private val overlayViews = HashMap<WebView, FrameLayout>()

        // ZERO-SIZE WINDOW HACK
        fun attachWebViewToOverlay(webView: WebView) {
            if (windowManager == null) {
                windowManager = webView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }
            if (overlayViews.containsKey(webView)) return
            
            val params = WindowManager.LayoutParams(
                1, 1, // 1x1 pixel window
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                else 
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, // Screen ko sone se rokega
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            params.x = 0
            params.y = 0
            
            val container = FrameLayout(webView.context)
            (webView.parent as? ViewGroup)?.removeView(webView)
            container.addView(webView)
            
            try {
                windowManager?.addView(container, params)
                overlayViews[webView] = container
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun detachWebViewFromOverlay(webView: WebView) {
            val container = overlayViews[webView] ?: return
            try {
                windowManager?.removeView(container)
                (webView.parent as? ViewGroup)?.removeView(webView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayViews.remove(webView)
        }
    }

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
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupWatchdogAlarm() // 🔥 CPU ko jagaye rakhne ke liye
    }

    private fun setupWatchdogAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HtmlService::class.java)
        val pendingIntent = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        // Har 1 minute me alarm bajega (OS ko lagatar jagata rahega)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60000, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 60000, pendingIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            isUserStopped = true
            WebViewManager.destroyAllWebViews()
            WebStorage.getInstance().deleteAllData()
            getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().clear().commit()
            stopSelf()
            return START_NOT_STICKY
        }

        // Watchdog ko reset karein
        setupWatchdogAlarm()

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
        val stopIntent = Intent(this, HtmlService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "html_alert_channel")
            .setContentTitle("App Running (100% Immortal)")
            .setContentText("Scripts are active in background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "STOP", stopPendingIntent) 
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 🔥 OS memory clear karne ki koshish karega, hum use ignore karenge
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Toast.makeText(applicationContext, "App background mein chalti rahegi...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
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
