package com.smsguard.startup

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object ProtectionServiceStarter {
    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, SmsProtectionService::class.java),
        )
    }
}
