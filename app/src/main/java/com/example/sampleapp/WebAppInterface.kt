package com.example.sampleapp

import android.app.NotificationManager
import android.content.Context
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat

class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun showRealTimeAlert(message: String) {
        val notification = NotificationCompat.Builder(context, "html_alert_channel")
            .setContentTitle("रियल-टाइम अलर्ट")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
