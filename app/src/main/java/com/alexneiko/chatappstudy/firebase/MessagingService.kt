package com.alexneiko.chatappstudy.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alexneiko.chatappstudy.R
import com.alexneiko.chatappstudy.models.User
import com.alexneiko.chatappstudy.ui.activities.ChatActivity
import com.alexneiko.chatappstudy.utilites.Constants
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

/** service for Cloud Messaging (in firebase) = need to register in manifest */
class MessagingService: FirebaseMessagingService() {

    override fun onNewToken(@NonNull token: String) {
        super.onNewToken(token)
        //Log.d("FCM", "Token: $token")
    }

    override fun onMessageReceived(@NonNull message: RemoteMessage) {
        super.onMessageReceived(message)
        //Log.d("FCM", "Message: ${message.notification?.body}")

        val user = User()
        user.id = message.data[Constants.KEY_USER_ID].toString()
        user.name = message.data[Constants.KEY_NAME].toString()
        user.token = message.data[Constants.KEY_FCM_TOKEN].toString()

        val notificationId = Random.nextInt()
        Log.d("FCM", "Random.nextInt(): $notificationId")

        val channelId = "chat_massage"

        val intent = Intent(this, ChatActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(Constants.KEY_USER, user)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val builder = NotificationCompat.Builder(this, channelId)
        builder.setSmallIcon(R.drawable.ic_notification)
        builder.setContentTitle(user.name)
        builder.setContentText(message.data.get(Constants.KEY_MESSAGE))
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(
            message.data.get(Constants.KEY_MESSAGE)
        ))
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        builder.setContentIntent(pendingIntent)
        builder.setAutoCancel(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName: CharSequence = "Chat Message"
            val channelDescription: String = "This notification channel is used for chat message notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            channel.description = channelDescription
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManagerCompat.notify(notificationId, builder.build())
    }

}
