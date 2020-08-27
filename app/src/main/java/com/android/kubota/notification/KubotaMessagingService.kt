package com.android.kubota.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.android.kubota.app.AppProxy
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.inmotionsoftware.promisekt.ensure
import com.kubota.service.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KubotaMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            val channelId = "fcm_default_channel"
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder =
                NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(it.title)
                    .setContentText(it.body)
                    .setAutoCancel(true)
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

            notificationManager.notify(0, notificationBuilder.build())
        }
    }

    override fun onNewToken(token: String) {
        val latch = CountDownLatch(1)
        @SuppressLint("HardwareIds")
        val deviceId = Settings.Secure.getString(AppProxy.proxy.contentResolver, Settings.Secure.ANDROID_ID)
        AppProxy.proxy.serviceManager.userPreferenceService.registerFCMToken(token = token, deviceId = deviceId)
            .ensure {
                AppProxy.proxy.fcmToken = token
                latch.countDown()
            }

        latch.await(60, TimeUnit.SECONDS)
    }
}