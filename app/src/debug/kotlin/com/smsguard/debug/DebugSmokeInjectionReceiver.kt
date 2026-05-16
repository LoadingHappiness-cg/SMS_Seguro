package com.smsguard.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smsguard.core.AppLogger
import com.smsguard.notification.SmsEventProcessor

class DebugSmokeInjectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INJECT) return

        val text = intent.getStringExtra(EXTRA_TEXT)?.trim().orEmpty()
        if (text.isBlank()) {
            AppLogger.w("DebugSmoke ignored reason=blank_text")
            return
        }

        val sender = intent.getStringExtra(EXTRA_SENDER)?.trim().orEmpty().ifBlank { DEFAULT_SENDER }
        val source = intent.getStringExtra(EXTRA_SOURCE)?.trim().orEmpty().ifBlank { DEFAULT_SOURCE }

        AppLogger.d("DebugSmoke inject sender=$sender source=$source")
        SmsEventProcessor.enqueueProcess(
            context = context.applicationContext,
            sender = sender,
            rawText = text,
            source = source,
        )
    }

    companion object {
        const val ACTION_INJECT = "com.smsguard.debug.SMOKE_INJECT"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_TEXT = "text"
        const val EXTRA_SOURCE = "source"

        private const val DEFAULT_SENDER = "Debug Smoke"
        private const val DEFAULT_SOURCE = "debug_smoke"
    }
}
