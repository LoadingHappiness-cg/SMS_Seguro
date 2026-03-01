package com.smsguard.notification

import android.app.Notification
import android.content.ComponentName
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.smsguard.R
import com.smsguard.core.AppLogger
import com.smsguard.core.UrlExtractor

class SmsNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        AppLogger.d("Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        AppLogger.w("Notification listener disconnected; requesting rebind")
        try {
            requestRebind(ComponentName(this, SmsNotificationListener::class.java))
        } catch (e: Exception) {
            AppLogger.e("Failed to request listener rebind", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return
            if (!SmsSourceAllowlist.isAllowed(applicationContext, pkg)) {
                AppLogger.d("ProbeA intake ignored source=notification_listener package=$pkg reason=package_not_allowed")
                return
            }

            val extras = sbn.notification.extras
            val title =
                extras.getString(Notification.EXTRA_TITLE)
                    ?: applicationContext.getString(R.string.unknown)
            val fullText = extractNotificationText(extras)
            if (fullText.isBlank()) {
                AppLogger.d("ProbeA intake dropped source=notification_listener package=${sbn.packageName} reason=no_extractable_text category=${sbn.notification.category}")
                return
            }

            val primaryDomain =
                UrlExtractor.extractUrls(fullText)
                    .firstOrNull()
                    ?.let(UrlExtractor::getDomain)
                    .orEmpty()

            AppLogger.d("ProbeA event received source=notification_listener package=$pkg domain=$primaryDomain timestamp=${System.currentTimeMillis()}")
            AlertPipelineDiagnostics.recordEvent(
                context = applicationContext,
                source = "notification_listener",
                packageName = pkg,
            )

            SmsEventProcessor.process(
                context = applicationContext,
                sender = title,
                rawText = fullText,
                source = "notification_listener",
                packageName = pkg,
            )
        } catch (e: Exception) {
            AppLogger.e("ProbeA exception while processing SMS notification", e)
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
