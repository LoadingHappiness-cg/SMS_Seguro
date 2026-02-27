package com.smsguard.notification

import android.app.Notification
import android.content.ComponentName
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smsguard.R

class SmsNotificationListener : NotificationListenerService() {

    private val supportedPackages = setOf(
        "com.google.android.apps.messaging",
        "com.android.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",

        // Xiaomi / MIUI variants (some regions still ship a Xiaomi-branded SMS app)
        "com.miui.mms",
        "com.miui.sms",
        "com.xiaomi.mms",
        "com.xiaomi.sms",
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("SMS_SEGURO", "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("SMS_SEGURO", "Notification listener disconnected; requesting rebind")
        try {
            requestRebind(ComponentName(this, SmsNotificationListener::class.java))
        } catch (e: Exception) {
            Log.e("SMS_SEGURO", "Failed to request listener rebind", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val isLikelyMessageNotification = sbn.notification.category == Notification.CATEGORY_MESSAGE

            if (!supportedPackages.contains(sbn.packageName) && !isLikelyMessageNotification) {
                return
            }

            val extras = sbn.notification.extras
            val title =
                extras.getString(Notification.EXTRA_TITLE)
                    ?: applicationContext.getString(R.string.unknown)
            val fullText = extractNotificationText(extras)
            if (fullText.isBlank()) {
                Log.d(
                    "SMS_SEGURO",
                    "Message notification without extractable text. package=${sbn.packageName} category=${sbn.notification.category}",
                )
                return
            }

            SmsEventProcessor.process(
                context = applicationContext,
                sender = title,
                rawText = fullText,
                source = "notification_listener",
                packageName = sbn.packageName,
            )
        } catch (e: Exception) {
            Log.e("SMS_SEGURO", "Error while processing SMS notification", e)
        }
    }

    private fun extractNotificationText(extras: Bundle): String {
        val parts = mutableListOf<String>()

        extras.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let(parts::add)

        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?.let(parts::add)

        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.map { it?.toString().orEmpty() }
            ?.map(String::trim)
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { lines ->
                parts.add(lines.joinToString("\n"))
            }

        extractMessagingStyleTexts(extras, Notification.EXTRA_MESSAGES).forEach(parts::add)
        extractMessagingStyleTexts(extras, Notification.EXTRA_HISTORIC_MESSAGES).forEach(parts::add)

        return parts
            .distinct()
            .joinToString("\n")
            .trim()
    }

    private fun extractMessagingStyleTexts(extras: Bundle, key: String): List<String> {
        val parcelables = extras.getParcelableArrayCompat(key)
        if (parcelables.isNullOrEmpty()) return emptyList()

        val fromBundles =
            parcelables
                .mapNotNull { it as? Bundle }
                .mapNotNull { bundle ->
                    bundle.getCharSequence("text")
                        ?.toString()
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                }
        if (fromBundles.isNotEmpty()) return fromBundles.distinct()

        // Some OEM/framework variants provide parcelables with getText() instead of raw bundles.
        return parcelables.mapNotNull { parcelable ->
            runCatching {
                val method = parcelable.javaClass.methods.firstOrNull { it.name == "getText" }
                method?.invoke(parcelable) as? CharSequence
            }.getOrNull()
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.distinct()
    }

    private fun Bundle.getParcelableArrayCompat(key: String): Array<Parcelable>? {
        return getParcelableArray(key)
    }
}
