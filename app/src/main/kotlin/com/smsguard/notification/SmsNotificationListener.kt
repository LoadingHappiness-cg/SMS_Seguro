package com.smsguard.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smsguard.R
import com.smsguard.core.AlertType
import com.smsguard.core.RuleSet
import com.smsguard.core.HistoryEvent
import com.smsguard.core.MultibancoDetector
import com.smsguard.core.RiskEngine
import com.smsguard.core.RiskAssessment
import com.smsguard.core.RiskLevel
import com.smsguard.core.UrlExtractor
import com.smsguard.rules.RuleLoader
import com.smsguard.storage.HistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var ruleLoader: RuleLoader
    private lateinit var historyStore: HistoryStore

    @Volatile
    private var cachedRulesetVersion: Int = -1

    @Volatile
    private var cachedRuleSet: RuleSet? = null

    @Volatile
    private var cachedRiskEngine: RiskEngine? = null

    override fun onCreate() {
        super.onCreate()
        ruleLoader = RuleLoader(applicationContext)
        historyStore = HistoryStore(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        ioScope.coroutineContext.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("SMS_SEGURO", "Notification listener connected")
        ioScope.launch {
            ensureRiskEngine()
        }
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
            }
            val text = if (fullText.length > 2_000) fullText.substring(0, 2_000) else fullText

            val mbData = MultibancoDetector.detect(text)
            val urls = UrlExtractor.extractUrls(text)
            if (urls.isEmpty() && mbData == null) return

            val riskEngine = ensureRiskEngine()

        var bestUrl: String = ""
        var bestDomain: String = ""
        var bestResultScore = 0
        var bestResultReasons: List<String> = emptyList()

        for (url in urls) {
            val result = riskEngine.analyze(url, text)
            if (result.score >= bestResultScore) {
                bestResultScore = result.score
                bestResultReasons = result.reasons
                bestUrl = url
                bestDomain = UrlExtractor.getDomain(url)
            }
        }

        val mbScoreBase = 30
        val mbScore =
            if (mbData != null) mbScoreBase + if (!mbData.valor.isNullOrBlank()) 10 else 0 else 0
        val mbReasonCode = "multibanco_payment"

        val finalScore = maxOf(bestResultScore, mbScore)
        val computedLevel =
            when {
                finalScore >= 70 -> RiskLevel.HIGH
                finalScore >= 40 -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }
        val finalLevel =
            if (mbData != null && computedLevel == RiskLevel.LOW) RiskLevel.MEDIUM else computedLevel

        val reasons =
            buildList {
                if (mbData != null) add(mbReasonCode)
                addAll(bestResultReasons)
            }.distinct()

        val alertType = if (mbData != null) AlertType.MULTIBANCO else AlertType.URL

        val assessment =
            RiskAssessment(
                alertType = alertType,
                primaryUrl = bestUrl,
                primaryDomain = bestDomain,
                score = finalScore,
                level = finalLevel,
                reasons = reasons,
                multibancoData = mbData
            )

        val strongUrlReasons =
            setOf(
                "shortener",
                "suspicious_tld",
                "punycode",
                "brand_impersonation",
                "weird_structure",
            )

        val lowRiskButSuspiciousUrl =
            assessment.alertType == AlertType.URL &&
                (
                    assessment.reasons.any { it in strongUrlReasons } ||
                        (assessment.score >= 20 && assessment.reasons.size >= 2)
                )

        // Show alert notification when:
        // - Multibanco payment request is detected, or
        // - URL risk is MEDIUM/HIGH, or
        // - URL risk is LOW but includes strong suspicious signals.
        val shouldNotify =
            assessment.alertType == AlertType.MULTIBANCO ||
                assessment.level == RiskLevel.HIGH ||
                assessment.level == RiskLevel.MEDIUM ||
                lowRiskButSuspiciousUrl

        Log.d(
            "SMS_SEGURO",
            "Assessment package=${sbn.packageName} urls=${urls.size} score=${assessment.score} level=${assessment.level} reasons=${assessment.reasons.joinToString(",")} shouldNotify=$shouldNotify",
        )

        if (shouldNotify) {
            Log.d("SMS_SEGURO", "Alert triggered: ${assessment.level} (${assessment.alertType})")

            AlertNotifier.show(
                applicationContext,
                title,
                assessment
            )
        }

        // Save to history (atomic write on IO dispatcher)
            val event =
                HistoryEvent(
                    timestamp = System.currentTimeMillis(),
                    sender = title,
                    domain = bestDomain,
                    url = bestUrl.takeIf { it.isNotBlank() },
                    alertType = alertType,
                    multibancoEntidade = mbData?.entidade,
                    multibancoReferencia = mbData?.referencia,
                    multibancoValor = mbData?.valor,
                    score = finalScore,
                    riskLevel = finalLevel,
                    reasons = reasons
                )

            ioScope.launch {
                historyStore.saveEvent(event)
            }
        } catch (e: Exception) {
            Log.e("SMS_SEGURO", "Error while processing SMS notification", e)
        }
    }

    private fun ensureRiskEngine(): RiskEngine {
        val prefs = applicationContext.getSharedPreferences("ruleset_meta", MODE_PRIVATE)
        val version = prefs.getInt("ruleset_version", -1)

        val engine = cachedRiskEngine
        if (engine != null && version == cachedRulesetVersion) return engine

        synchronized(this) {
            val currentEngine = cachedRiskEngine
            if (currentEngine != null && version == cachedRulesetVersion) return currentEngine

            val ruleSet = ruleLoader.loadCurrent()
            cachedRuleSet = ruleSet
            cachedRulesetVersion = ruleSet.version
            val newEngine = RiskEngine(ruleSet)
            cachedRiskEngine = newEngine
            return newEngine
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
