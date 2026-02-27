package com.smsguard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.smsguard.R

object AlertNotifierChannels {

    const val CHANNEL_ID = "smsguard_alerts"

    fun ensure(context: Context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val existing = nm.getNotificationChannel(CHANNEL_ID)

        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description = context.getString(R.string.alert_channel_description)
        channel.enableVibration(true)

        nm.createNotificationChannel(channel)
    }
}
