package com.smsguard.startup

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import com.smsguard.notification.SmsNotificationListener
import com.smsguard.update.RuleUpdateScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d("SMS_SEGURO", "Boot/package event received: ${intent.action}")
                RuleUpdateScheduler.schedulePeriodic(context)
                requestNotificationListenerRebind(context)
                startProtectionService(context)
            }
        }
    }

    private fun requestNotificationListenerRebind(context: Context) {
        try {
            val component = ComponentName(context, SmsNotificationListener::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationListenerService.requestRebind(component)
            }
        } catch (e: Exception) {
            Log.e("SMS_SEGURO", "Failed to rebind notification listener after boot", e)
        }
    }

    private fun startProtectionService(context: Context) {
        val serviceIntent = Intent(context, SmsProtectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
