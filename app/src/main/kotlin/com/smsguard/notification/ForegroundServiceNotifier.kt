package com.smsguard.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.smsguard.R
import com.smsguard.ui.MainActivity

object ForegroundServiceNotifier {

    const val CHANNEL_ID = "sms-guard-foreground"
    const val NOTIFICATION_ID = 12001

    internal data class ForegroundNotificationContent(
        @androidx.annotation.StringRes val titleResId: Int,
        @androidx.annotation.StringRes val textResId: Int,
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.foreground_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.foreground_notification_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        ensureChannel(context)
        val content = contentFor(
            discreetMode = ForegroundNotificationPreferences.isDiscreetModeEnabled(context),
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(content.titleResId))
            .setContentText(context.getString(content.textResId))
            .setContentIntent(buildOpenProtectionPendingIntent(context))
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()
    }

    internal fun contentFor(discreetMode: Boolean): ForegroundNotificationContent =
        if (discreetMode) {
            ForegroundNotificationContent(
                titleResId = R.string.foreground_notification_title_discreet,
                textResId = R.string.foreground_notification_text_discreet,
            )
        } else {
            ForegroundNotificationContent(
                titleResId = R.string.foreground_notification_title,
                textResId = R.string.foreground_notification_text,
            )
        }

    fun isNotificationVisible(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false

        return manager.activeNotifications.any { notification ->
            notification.id == NOTIFICATION_ID
        }
    }

    private fun buildOpenProtectionPendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_TAB, "protecao")
            }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
