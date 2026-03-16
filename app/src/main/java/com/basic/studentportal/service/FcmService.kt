package com.basic.studentportal.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.basic.studentportal.R
import com.basic.studentportal.data.local.TokenDataStore
import com.basic.studentportal.ui.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenDataStore: TokenDataStore

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            tokenDataStore.saveFcmToken(token)
            val authToken = tokenDataStore.getAuthToken().firstOrNull()
            if (!authToken.isNullOrEmpty()) {
                fcmTokenRepository.sendTokenToServer(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Basic Academy"
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""
        val type  = message.data["type"] ?: "general"
        showNotification(title, body, type)
    }

    private fun showNotification(title: String, body: String, type: String) {
        val channelId   = getChannelId(type)
        val channelName = getChannelName(type)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getChannelId(type: String) = when (type) {
        "payment"  -> "channel_payment"
        "notice"   -> "channel_notice"
        "due"      -> "channel_due"
        "routine"  -> "channel_routine"
        "material" -> "channel_material"
        "invoice"  -> "channel_invoice"
        else       -> "channel_general"
    }

    private fun getChannelName(type: String) = when (type) {
        "payment"  -> "Payment Notifications"
        "notice"   -> "Notice Notifications"
        "due"      -> "Due Alert Notifications"
        "routine"  -> "Routine Notifications"
        "material" -> "Study Material Notifications"
        "invoice"  -> "Invoice Notifications"
        else       -> "General Notifications"
    }
}
