package com.example.sampleapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.Locale

class HtmlService : Service(), TextToSpeech.OnInitListener {
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var isUserStopped = false 
    private var server: LocalServer? = null
    
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    
    // Heartbeat के लिए
    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val activeWebViews = mutableListOf<WebView>()

    companion object {
        private var windowManager: WindowManager? = null
        private val overlayViews = HashMap<WebView, FrameLayout>()
        private var instance: HtmlService? = null

        // HTML से अलर्ट आने पर यह कॉल होगा
        fun triggerNativeAlert(message: String) {
            instance?.playNativeAlert(message)
        }

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
            } catch (e: Exception) { e.printStackTrace() }
        }

        fun detachWebViewFromOverlay(webView: WebView) {
            val container = overlayViews[webView] ?: return
            try {
                windowManager?.removeView(container)
                (webView.parent as? ViewGroup)?.removeView(webView)
            } catch (e: Exception) { e.printStackTrace() }
            overlayViews.remove(webView)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        try { startForeground(1, buildNotification()) } catch (e: Exception) {}

        tts = TextToSpeech(this, this)

        if (server == null) {
            server = LocalServer(applicationContext, 8080)
            try { server?.start() } catch (e: Exception) { e.printStackTrace() }
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScriptApp::WakeLock")
            wakeLock?.acquire() 
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ScriptApp::WifiLock")
            wifiLock?.acquire()
        }
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // 🔥 Heartbeat शुरू करें
        startHeartbeat()
    }

    // यह फंक्शन हर 5 सेकंड में WebView को जगाएगा
    private fun startHeartbeat() {
        heartbeatHandler.postDelayed(object : Runnable {
            override fun run() {
                for (wv in activeWebViews) {
                    // अगर WebSocket डिस्कनेक्ट हुआ है, तो दोबारा कनेक्ट करने का कमांड भेजें
                    wv.evaluateJavascript("""
                        if (typeof connect === 'function' && (!state.ws || state.ws.readyState !== 1)) {
                            console.log('Native Wakeup: Reconnecting...');
                            connect();
                        }
                    """.trimIndent(), null)
                }
                heartbeatHandler.postDelayed(this, 5000)
            }
        }, 5000)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    // असली आवाज़ और बीप बजाने वाला फंक्शन
    fun playNativeAlert(message: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            mediaPlayer?.start()
            
            tts?.stop()
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "Alert")
        } catch (e: Exception) {
            e.printStackTrace()
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

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        for ((key, value) in prefs.all) {
            if (key.startsWith("run_") && value == true) { 
                val savedScript = key.replace("run_", "")
                val file = File(filesDir, "scripts/$savedScript")
                if (file.exists()) {
                    val wv = WebViewManager.getOrCreateWebView(applicationContext, savedScript)
                    if (!activeWebViews.contains(wv)) activeWebViews.add(wv)
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
        heartbeatHandler.removeCallbacksAndMessages(null)
        tts?.shutdown()
        mediaPlayer?.release()

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
        if (wifiLock?.isHeld == true) wifiLock?.release()
        super.onDestroy()
    }
}
