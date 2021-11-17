package com.android.kubota.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.style.ForegroundColorSpan
import androidx.core.app.NotificationCompat
import androidx.core.text.inSpans
import com.android.kubota.app.AppProxy
import com.android.kubota.ui.MainActivity
import com.android.kubota.ui.notification.parseNotifications
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.inmotionsoftware.flowkit.android.put
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KubotaMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let { notification ->
            val channelId = "fcm_default_channel"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val activityIntent = PendingIntent.getActivity(
                this,
                remoteMessage.sentTime.toInt(),
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    val initial = Bundle()
                    initial.putString("body", notification.body)
                    initial.putString("title", notification.title)
                    putExtras(remoteMessage.data.entries.fold(initial) { acc, entry ->
                        acc.put(entry.key, entry.value)
                    })
                },
                0
            )

            val notificationBuilder =
                NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(notification.title)
                    .setContentText(notification.body)
                    .setAutoCancel(true)
                    .setContentIntent(activityIntent)
                    .setSound(defaultSoundUri)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create notification channel
                val channel = NotificationChannel(
                    channelId,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(remoteMessage.sentTime.toInt(), notificationBuilder.build())
        }
    }

    override fun onNewToken(token: String) {
        val latch = CountDownLatch(1)

        @SuppressLint("HardwareIds")
        val deviceId =
            Settings.Secure.getString(AppProxy.proxy.contentResolver, Settings.Secure.ANDROID_ID)
        AppProxy.proxy.serviceManager.userPreferenceService.registerFCMToken(
            token = token,
            deviceId = deviceId
        )
            .ensure {
                AppProxy.proxy.fcmToken = token
                latch.countDown()
            }

        latch.await(60, TimeUnit.SECONDS)
    }
}