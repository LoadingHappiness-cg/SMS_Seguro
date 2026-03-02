package com.smsguard.core

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smsguard.notification.AlertNotifierChannels
import com.smsguard.notification.ForegroundServiceNotifier

data class ProtectionStatusReport(
    val listenerOk: Boolean,
    val notificationsAllowed: Boolean,
    val postNotificationsOk: Boolean,
    val alertChannelOk: Boolean,
    val foregroundNotificationAllowed: Boolean,
    val foregroundChannelOk: Boolean,
    val foregroundNotificationVisible: Boolean,
) {
    val alertsReady: Boolean
        get() = notificationsAllowed && postNotificationsOk && alertChannelOk

    val protectionRunning: Boolean
        get() = listenerOk && alertsReady

    val isReady: Boolean
        get() = protectionRunning && foregroundNotificationAllowed
}

class PermissionHealth(
    private val context: Context,
) {
    val postNotificationsGranted: Boolean
        get() = NotificationPermission.isGranted(context)

    val needsPostNotifications: Boolean
        get() {
            return !postNotificationsGranted
        }

    val notificationsEnabled: Boolean
        get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    val hasNotificationListenerAccess: Boolean
        get() {
            val flat =
                Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners",
                )
                    ?: return false

            if (flat.isBlank()) return false

            val pkgName = context.packageName
            return flat.split(":").any { flattened ->
                val cn = ComponentName.unflattenFromString(flattened) ?: return@any false
                cn.packageName == pkgName
            }
        }

    val isIgnoringBatteryOptimizations: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }

    val alertChannelOk: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return false

            val channel = notificationManager.getNotificationChannel(AlertNotifierChannels.CHANNEL_ID)
                ?: return false

            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }

    val foregroundNotificationChannelOk: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return false

            val channel = notificationManager.getNotificationChannel(ForegroundServiceNotifier.CHANNEL_ID)
                ?: return false

            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }

    val foregroundNotificationVisible: Boolean
        get() {
            if (!isForegroundNotificationAllowed()) return false
            return ForegroundServiceNotifier.isNotificationVisible(context)
        }

    fun isForegroundNotificationAllowed(): Boolean {
        return notificationsEnabled && !needsPostNotifications && foregroundNotificationChannelOk
    }

    fun protectionStatusReport(): ProtectionStatusReport =
        protectionStatusReport(
            listenerOk = hasNotificationListenerAccess,
            notificationsAllowed = notificationsEnabled,
            postNotificationsOk = !needsPostNotifications,
            alertChannelOk = alertChannelOk,
            foregroundNotificationAllowed = isForegroundNotificationAllowed(),
            foregroundChannelOk = foregroundNotificationChannelOk,
            foregroundNotificationVisible = foregroundNotificationVisible,
        )

    fun isProtectionReady(): Boolean {
        return protectionStatusReport().isReady
    }
}

internal fun protectionStatusReport(
    listenerOk: Boolean,
    notificationsAllowed: Boolean,
    postNotificationsOk: Boolean,
    alertChannelOk: Boolean,
    foregroundNotificationAllowed: Boolean,
    foregroundChannelOk: Boolean = foregroundNotificationAllowed,
    foregroundNotificationVisible: Boolean = foregroundNotificationAllowed,
): ProtectionStatusReport =
    ProtectionStatusReport(
        listenerOk = listenerOk,
        notificationsAllowed = notificationsAllowed,
        postNotificationsOk = postNotificationsOk,
        alertChannelOk = alertChannelOk,
        foregroundNotificationAllowed = foregroundNotificationAllowed,
        foregroundChannelOk = foregroundChannelOk,
        foregroundNotificationVisible = foregroundNotificationVisible,
    )
