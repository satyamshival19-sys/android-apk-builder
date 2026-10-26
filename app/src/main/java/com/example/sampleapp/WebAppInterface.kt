package com.example.sampleapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat

class WebAppInterface(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 999

    @JavascriptInterface
    fun showAlertWithSound(message: String) {
        val channelId = "html_alert_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 ALERT! 🚨")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()
        notificationManager.notify(notificationId, notification)

        stopAndroidAlert()

        try {
            val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    @JavascriptInterface
    fun stopAndroidAlert() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
                mediaPlayer = null
            }
            notificationManager.cancel(notificationId)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
