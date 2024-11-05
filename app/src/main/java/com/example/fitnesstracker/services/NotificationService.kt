package com.example.fitnesstracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R

class NotificationService : Service(){

    private val CHANNEL_ID = "notification_channel"
    private val notificationID = 101

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkNotificationSettings(this)
        // Return START_STICKY to keep the service running if the system kills it
        return START_STICKY
    }

    fun checkNotificationSettings(context: Context) {
        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val notificationsEnabled = sharedPreferences.getBoolean("notifications", false)

        val notificationVolume = sharedPreferences.getInt("volume_notifications", 60)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notificationVolume, 0)

        if (notificationsEnabled) {
            createNotificationChannel()
            sendNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification name"
            val descriptionText = "Notification text"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val stepsTaken = sharedPreferences.getInt("currentSteps", 0)
        val caloriesBurned = sharedPreferences.getFloat("totalCalories", (stepsTaken.toFloat()*0.04).toFloat())
        val distanceWalked = sharedPreferences.getFloat("totalDistance", (stepsTaken.toFloat()*80)/100000)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val bitmapLargeIcon = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.stats)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle("Steps walked: $stepsTaken")
            .setContentText("Calories burned: ${caloriesBurned}kcal - Distance: ${distanceWalked}km")
            .setLargeIcon(bitmapLargeIcon)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Calories burned: ${caloriesBurned} kcal \nDistance: ${distanceWalked} km \nKEEP GOING! YOU CAN DO IT!"))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notify(notificationID, builder.build())
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}