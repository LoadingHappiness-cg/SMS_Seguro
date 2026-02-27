package com.smsguard.core

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class PermissionHealth(
    private val context: Context,
) {
    val needsPostNotifications: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

    val notificationsEnabled: Boolean
        get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    val hasReceiveSmsPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

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

    fun isProtectionReady(): Boolean {
        val notificationsOk = !needsPostNotifications && notificationsEnabled
        return notificationsOk && hasNotificationListenerAccess
    }
}
