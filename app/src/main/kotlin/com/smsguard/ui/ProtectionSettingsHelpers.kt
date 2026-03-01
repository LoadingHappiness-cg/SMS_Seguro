package com.smsguard.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import com.smsguard.R
import com.smsguard.core.ProtectionStatusReport
import com.smsguard.core.xiaomiSupportInfo
import com.smsguard.notification.AlertNotifierChannels
import com.smsguard.notification.ForegroundServiceNotifier

internal fun Context.openIntentSafely(
    intent: Intent,
    fallback: () -> Unit = { openAppDetails() },
) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        fallback()
    }
}

internal fun Context.openAppDetails() {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
    openIntentSafely(intent)
}

internal fun Context.openAppNotificationSettings() {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
    openIntentSafely(intent)
}

internal fun Context.openAlertChannelSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent =
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, AlertNotifierChannels.CHANNEL_ID)
            }
        openIntentSafely(intent) { openAppNotificationSettings() }
        return
    }

    openAppNotificationSettings()
}

internal fun Context.openForegroundChannelSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent =
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, ForegroundServiceNotifier.CHANNEL_ID)
            }
        openIntentSafely(intent) { openAppNotificationSettings() }
        return
    }

    openAppNotificationSettings()
}

internal fun Context.fixForegroundNotification(report: ProtectionStatusReport) {
    when {
        !report.alertsReady -> openAlertDeliverySettings(report)
        !report.foregroundNotificationAllowed -> openForegroundChannelSettings()
        else -> openAppNotificationSettings()
    }
}

internal fun Context.openAlertDeliverySettings(report: ProtectionStatusReport) {
    when {
        !report.notificationsAllowed || !report.postNotificationsOk -> openAppNotificationSettings()
        !report.alertChannelOk -> openAlertChannelSettings()
        else -> openAppNotificationSettings()
    }
}

internal fun Context.openBatteryOptimizationSettings() {
    val info = xiaomiSupportInfo()
    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isIgnoring =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(packageName) == true
        } else {
            true
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoring) {
        val requestIntent =
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        openIntentSafely(requestIntent) { openBatteryOptimizationList() }
        return
    }

    openBatteryOptimizationList()

    if (info.shouldShowGuidance) {
        Toast.makeText(this, getString(R.string.xiaomi_battery_toast), Toast.LENGTH_LONG).show()
    }
}

private fun Context.openBatteryOptimizationList() {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    openIntentSafely(intent) { openAppDetails() }
}

internal fun Context.openRestrictedPermissionsHelp() {
    openAppDetails()
    Toast.makeText(this, getString(R.string.xiaomi_restricted_permissions_toast), Toast.LENGTH_LONG).show()
}

internal fun Context.openXiaomiAutoStartSettings() {
    val candidates =
        listOf(
            Intent("miui.intent.action.OP_AUTO_START").apply {
                component =
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity",
                    )
            },
            Intent().apply {
                component =
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity",
                    )
                putExtra("extra_pkgname", packageName)
            },
        )

    val opened =
        candidates.any { candidate ->
            runCatching {
                startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
        }

    if (!opened) {
        openAppDetails()
    }

    Toast.makeText(this, getString(R.string.xiaomi_autostart_toast), Toast.LENGTH_LONG).show()
}
