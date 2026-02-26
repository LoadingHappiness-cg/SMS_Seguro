package com.smsguard.notification

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.smsguard.core.HistoryEvent
import com.smsguard.core.RiskEngine
import com.smsguard.core.UrlExtractor
import com.smsguard.rules.RuleLoader
import com.smsguard.storage.HistoryStore
import com.smsguard.ui.AlertActivity

class SmsNotificationListener : NotificationListenerService() {

    private val supportedPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!supportedPackages.contains(sbn.packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val urls = UrlExtractor.extractUrls(text)
        if (urls.isEmpty()) return

        val ruleLoader = RuleLoader(applicationContext)
        val ruleSet = ruleLoader.loadCurrent()
        val riskEngine = RiskEngine(ruleSet)
        val historyStore = HistoryStore(applicationContext)

        for (url in urls) {
            val result = riskEngine.analyze(url, text)
            
            // Save to history
            val event = HistoryEvent(
                timestamp = System.currentTimeMillis(),
                sender = title,
                domain = UrlExtractor.getDomain(url),
                score = result.score,
                riskLevel = result.level,
                reasons = result.reasons
            )
            historyStore.saveEvent(event)

            // Show alert if risk is Medium or High
            if (result.level != com.smsguard.core.RiskLevel.LOW) {
                val intent = Intent(applicationContext, AlertActivity::class.java).apply {
                    putExtra("risk_level", result.level.name)
                    putExtra("domain", event.domain)
                    putExtra("reasons", result.reasons.toTypedArray())
                    putExtra("url", url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
        }
    }
}
