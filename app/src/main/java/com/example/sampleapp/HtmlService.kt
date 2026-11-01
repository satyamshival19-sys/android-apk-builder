package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.wifi.WifiManager
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
import java.io.File

class HtmlService : Service() {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null // 🔥 NAYA: Wi-Fi ko zinda rakhne ke liye
    private var isUserStopped = false 
    private var server: LocalServer? = null

    companion object {
        private var windowManager: WindowManager? = null
        private val overlayViews = HashMap<WebView, FrameLayout>()

        fun attachWebViewToOverlay(webView: WebView) {
            if (windowManager == null) {
                windowManager = webView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }
            if (overlayViews.containsKey(webView)) return
            
            val params = WindowManager.LayoutParams(
                1, 1, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                else 
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
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

        // 🔥 WIFI LOCK: Ye OS ko bolta hai ki Wi-Fi ko mat sulao
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ScriptApp::WifiLock")
            wifiLock?.acquire()
        }
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupWatchdogAlarm()
    }

    private fun setupWatchdogAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, HtmlService::class.java)
        val pendingIntent = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, android.os.SystemClock.elapsedRealtime() + 60000, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, android.os.SystemClock.elapsedRealtime() + 60000, pendingIntent)
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
            .setContentTitle("App Running (Absolute Shield)")
            .setContentText("Charts and Alerts are fully active...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "STOP", stopPendingIntent) 
            .build()
    }

    override fun onDestroy() {
        if (!isUserStopped) {
            val myIntent = Intent(applicationContext, HtmlService::class.java)
            val pendingIntent = PendingIntent.getService(this, 1, myIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 1000, pendingIntent)
            } else {
                alarm.set(AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 1000, pendingIntent)
            }
        }
        server?.stop()
        server = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        if (wifiLock?.isHeld == true) wifiLock?.release() // 🔥 Wifi lock release
        super.onDestroy()
    }
}
