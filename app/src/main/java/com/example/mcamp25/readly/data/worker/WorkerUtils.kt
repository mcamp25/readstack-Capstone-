package com.example.mcamp25.readly.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "library_sync_channel"
private const val NOTIFICATION_ID = 1

fun makeStatusNotification(message: String, context: Context) {
    // 1. Create the NotificationChannel for API 26+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Library Sync"
        val descriptionText = "Shows notifications for library synchronization"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // 2. Build the notification
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_sync)
        .setContentTitle("Readly")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    // 3. Show the notification (Check permission for Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
    }

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
}
