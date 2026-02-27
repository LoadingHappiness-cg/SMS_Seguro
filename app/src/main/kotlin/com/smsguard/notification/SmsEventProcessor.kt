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
        if (urls.isEmpty() && mbData == null) return false

        val riskEngine = ensureRiskEngine(context)

        var bestUrl = ""
        var bestDomain = ""
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

        val mbScore = if (mbData != null) 30 + if (!mbData.valor.isNullOrBlank()) 10 else 0 else 0
        val finalScore = maxOf(bestResultScore, mbScore)
        val computedLevel =
            when {
                finalScore >= 70 -> RiskLevel.HIGH
                finalScore >= 40 -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }
        val finalLevel = if (mbData != null && computedLevel == RiskLevel.LOW) RiskLevel.MEDIUM else computedLevel

        val reasons =
            buildList {
                if (mbData != null) add("multibanco_payment")
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
                multibancoData = mbData,
            )

        val strongUrlReasons = setOf("shortener", "suspicious_tld", "punycode", "brand_impersonation", "weird_structure")
        val lowRiskButSuspiciousUrl =
            assessment.alertType == AlertType.URL &&
                (assessment.reasons.any { it in strongUrlReasons } || (assessment.score >= 20 && assessment.reasons.size >= 2))

        val shouldNotify =
            assessment.alertType == AlertType.MULTIBANCO ||
                assessment.level == RiskLevel.HIGH ||
                assessment.level == RiskLevel.MEDIUM ||
                lowRiskButSuspiciousUrl

        val eventKey = buildEventKey(text, assessment)
        if (isDuplicateEvent(context, eventKey)) {
            Log.d(
                "SMS_SEGURO",
                "Duplicate event suppressed source=$source package=${packageName.orEmpty()}",
            )
            return false
        }

        Log.d(
            "SMS_SEGURO",
            "Assessment source=$source package=${packageName.orEmpty()} urls=${urls.size} score=${assessment.score} level=${assessment.level} reasons=${assessment.reasons.joinToString(",")} shouldNotify=$shouldNotify",
        )

        if (shouldNotify) {
            AlertNotifier.show(context, sender, assessment)
        }

        val event =
            HistoryEvent(
                timestamp = System.currentTimeMillis(),
                sender = sender,
                domain = bestDomain,
                url = bestUrl.takeIf { it.isNotBlank() },
                alertType = alertType,
                multibancoEntidade = mbData?.entidade,
                multibancoReferencia = mbData?.referencia,
                multibancoValor = mbData?.valor,
                score = finalScore,
                riskLevel = finalLevel,
                reasons = reasons,
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

    private fun buildEventKey(
        text: String,
        assessment: RiskAssessment,
    ): String {
        val normalizedText = text.lowercase().replace("\\s+".toRegex(), " ").take(180)
        val mb =
            assessment.multibancoData?.let {
                "${it.entidade.orEmpty()}|${it.referencia.orEmpty()}|${it.valor.orEmpty()}"
            }.orEmpty()
        return listOf(
            assessment.alertType.name,
            normalizedText,
            assessment.primaryUrl.lowercase(),
            assessment.primaryDomain.lowercase(),
            mb,
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
