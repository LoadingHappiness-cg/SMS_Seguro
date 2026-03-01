package com.smsguard.startup

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import com.smsguard.notification.ForegroundServiceNotifier
import com.smsguard.notification.SmsNotificationListener
import com.smsguard.update.RuleUpdateScheduler

class SmsProtectionService : Service() {

    override fun onCreate() {
        super.onCreate()
        ForegroundServiceNotifier.ensureChannel(this)
        startForeground(ForegroundServiceNotifier.NOTIFICATION_ID, ForegroundServiceNotifier.build(this))
        RuleUpdateScheduler.schedulePeriodic(applicationContext)
        requestListenerRebind()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        requestListenerRebind()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun requestListenerRebind() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationListenerService.requestRebind(
                ComponentName(this, SmsNotificationListener::class.java),
            )
        }
    }
}
