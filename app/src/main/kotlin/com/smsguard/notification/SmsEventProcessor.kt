package com.smsguard.notification

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.smsguard.core.AlertType
import com.smsguard.core.HistoryEvent
import com.smsguard.core.MultibancoDetector
import com.smsguard.core.RiskAssessment
import com.smsguard.core.RiskEngine
import com.smsguard.core.RiskLevel
import com.smsguard.core.UrlExtractor
import com.smsguard.rules.RuleLoader
import com.smsguard.storage.HistoryStore

object SmsEventProcessor {

    private const val DEDUPE_PREFS = "sms_event_dedupe"
    private const val DEDUPE_LAST_KEY = "last_event_key"
    private const val DEDUPE_LAST_TIME_KEY = "last_event_time"
    private const val DEDUPE_WINDOW_MS = 15_000L

    @Volatile
    private var cachedRulesetVersion: Int = -1

    @Volatile
    private var cachedRiskEngine: RiskEngine? = null

    fun process(
        context: Context,
        sender: String,
        rawText: String,
        source: String,
        packageName: String? = null,
    ): Boolean {
        val text = rawText.trim().take(2_000)
        if (text.isBlank()) return false

        val mbData = MultibancoDetector.detect(text)
        val urls = UrlExtractor.extractUrls(text)

        val riskEngine = ensureRiskEngine(context)
        val result =
            riskEngine.analyze(
                messageText = text,
                urls = urls,
                multibancoData = mbData,
            )

        if (urls.isEmpty() && mbData == null && result.score == 0) {
            return false
        }

        val alertType = if (mbData != null) AlertType.MULTIBANCO else AlertType.URL
        val shouldNotify = result.level == RiskLevel.HIGH || result.level == RiskLevel.MEDIUM

        val eventKey = buildEventKey(text, result)
        if (isDuplicateEvent(context, eventKey)) {
            Log.d(
                "SMS_SEGURO",
                "Duplicate event suppressed source=$source package=${packageName.orEmpty()}",
            )
            return false
        }

        Log.d(
            "SMS_SEGURO",
            "Assessment source=$source package=${packageName.orEmpty()} urls=${urls.size} score=${result.score} level=${result.level} reasons=${result.reasons.joinToString(",")} shouldNotify=$shouldNotify",
        )

        if (shouldNotify) {
            AlertNotifier.show(
                context = context,
                sender = sender,
                assessment =
                    RiskAssessment(
                        alertType = alertType,
                        primaryUrl = result.primaryUrl,
                        primaryDomain = result.primaryDomain,
                        score = result.score,
                        level = result.level,
                        reasons = result.reasons,
                        multibancoData = mbData,
                    ),
            )
        }

        val event =
            HistoryEvent(
                timestamp = System.currentTimeMillis(),
                sender = sender,
                domain = result.primaryDomain,
                url = result.primaryUrl.takeIf { it.isNotBlank() },
                alertType = alertType,
                multibancoEntidade = mbData?.entidade,
                multibancoReferencia = mbData?.referencia,
                multibancoValor = mbData?.valor,
                score = result.score,
                riskLevel = result.level,
                reasons = result.reasons,
            )
        HistoryStore(context).saveEvent(event)
        return shouldNotify
    }

    private fun ensureRiskEngine(context: Context): RiskEngine {
        val prefs = context.getSharedPreferences("ruleset_meta", MODE_PRIVATE)
        val version = prefs.getInt("ruleset_version", -1)
        val engine = cachedRiskEngine
        if (engine != null && version == cachedRulesetVersion) return engine

        synchronized(this) {
            val current = cachedRiskEngine
            if (current != null && version == cachedRulesetVersion) return current
            val ruleSet = RuleLoader(context).loadCurrent()
            cachedRulesetVersion = ruleSet.version
            val created = RiskEngine(ruleSet)
            cachedRiskEngine = created
            return created
        }
    }

    private fun buildEventKey(text: String, result: RiskEngine.RiskResult): String {
        val normalizedText = text.lowercase().replace("\\s+".toRegex(), " ").take(180)
        return listOf(
            result.level.name,
            normalizedText,
            result.primaryUrl.lowercase(),
            result.primaryDomain.lowercase(),
        ).joinToString("|")
    }

    private fun isDuplicateEvent(context: Context, eventKey: String): Boolean {
        val prefs = context.getSharedPreferences(DEDUPE_PREFS, MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastKey = prefs.getString(DEDUPE_LAST_KEY, "").orEmpty()
        val lastTime = prefs.getLong(DEDUPE_LAST_TIME_KEY, 0L)
        val duplicate = lastKey == eventKey && now - lastTime <= DEDUPE_WINDOW_MS
        if (!duplicate) {
            prefs.edit()
                .putString(DEDUPE_LAST_KEY, eventKey)
                .putLong(DEDUPE_LAST_TIME_KEY, now)
                .apply()
        }
        return duplicate
    }
}
