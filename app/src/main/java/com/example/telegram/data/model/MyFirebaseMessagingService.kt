package com.example.telegram.data.model

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.example.telegram.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extract data from the message payload
        val title = remoteMessage.data["title"] ?: "New Message"
        val message = remoteMessage.data["message"] ?: "You have received a new message."

        showNotification(title, message)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "chat_messages_channel"
        val notificationId = 1

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+ (required)
        val channel = NotificationChannel(
            channelId,
            "Chat Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for new chat messages"
        }
        notificationManager.createNotificationChannel(channel)

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_google_logo) // use your app icon here
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Show the notification
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        // Called when a new FCM token is generated
        // You should send this token to your backend to send targeted notifications
        // For now, just log it or save it
        // Log.d("FCM", "New token: $token")
    }
}
