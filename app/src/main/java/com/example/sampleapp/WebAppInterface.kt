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
    
    // साउंड को बंद करने के लिए हमें इसे यहाँ रखना होगा
    private var mediaPlayer: MediaPlayer? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 999 // नोटिफिकेशन पहचानने के लिए आईडी

    @JavascriptInterface
    fun showAlertWithSound(message: String) {
        val channelId = "html_alert_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("अलर्ट!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(notificationId, notification)

        // पुराना साउंड बंद करें (अगर बज रहा है)
        stopAndroidAlert()

        // नया साउंड शुरू करें
        val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val volume = sharedPref.getInt("volume", 80) / 100f
        val soundUriString = sharedPref.getString("sound_uri", null)
        
        try {
            val uri: Uri = if (soundUriString != null) {
                Uri.parse(soundUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                setVolume(volume, volume)
                // ध्यान दें: हमने इसे Looping (लगातार बजने) के लिए सेट किया है
                isLooping = true 
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 👇 यह नया फंक्शन है जो HTML से साउंड और नोटिफिकेशन बंद करेगा
    @JavascriptInterface
    fun stopAndroidAlert() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.release()
                mediaPlayer = null
            }
            // नोटिफिकेशन भी हटा दें
            notificationManager.cancel(notificationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
