package com.smsguard.notification

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.smsguard.core.AppLogger
import com.smsguard.core.AlertType
import com.smsguard.core.HistoryEvent
import com.smsguard.core.MultibancoDetector
import com.smsguard.core.RiskAssessment
import com.smsguard.core.RiskEngine
import com.smsguard.core.RiskLevel
import com.smsguard.core.TextNormalizer
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
        if (text.isBlank()) {
            AppLogger.w("ProbeA intake dropped source=$source package=${packageName.orEmpty()} reason=blank_text")
            return false
        }
        val normalizedText = TextNormalizer.normalize(text)

        val mbData = MultibancoDetector.detect(normalizedText)
        val urls = UrlExtractor.extractUrls(text)

        val riskEngine = ensureRiskEngine(context)
        val result =
            riskEngine.analyze(
                messageText = text,
                normalizedText = normalizedText,
                urls = urls,
                multibancoData = mbData,
            )

        AppLogger.d("ProbeB risk evaluated source=$source package=${packageName.orEmpty()} level=${result.level} score=${result.score} reasons=${result.reasons.take(3).joinToString(",")} timestamp=${System.currentTimeMillis()}")
        AlertPipelineDiagnostics.recordRisk(
            context = context,
            level = result.level,
            score = result.score,
        )

        if (urls.isEmpty() && mbData == null && result.score == 0) {
            AppLogger.d("ProbeB event ignored source=$source package=${packageName.orEmpty()} reason=no_urls_no_multibanco_no_risk")
            return false
        }

        val alertType = if (mbData != null) AlertType.MULTIBANCO else AlertType.URL
        val shouldNotify = shouldNotify(alertType, result)

        val eventKey = buildEventKey(normalizedText, result)
        if (isDuplicateEvent(context, eventKey)) {
            AppLogger.d("ProbeC event suppressed source=$source package=${packageName.orEmpty()} reason=duplicate")
            return false
        }

        AppLogger.d("Assessment source=$source package=${packageName.orEmpty()} urls=${urls.size} score=${result.score} level=${result.level} reasons=${result.reasons.joinToString(",")} shouldNotify=$shouldNotify")

        val assessment =
            RiskAssessment(
                alertType = alertType,
                primaryUrl = result.primaryUrl,
                primaryDomain = result.primaryDomain,
                messageText = text,
                score = result.score,
                level = result.level,
                reasons = result.reasons,
                multibancoData = mbData,
            )

        val event =
            HistoryEvent(
                timestamp = System.currentTimeMillis(),
                sender = sender,
                messageText = text,
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
        val historyStore = HistoryStore(context)

        return persistAndMaybeNotify(
            event = event,
            shouldNotify = shouldNotify,
            persistEvent = { historyEvent ->
                AppLogger.d("ProbeC alert persisted riskLevel=${historyEvent.riskLevel} timestamp=${historyEvent.timestamp}")
                AlertPipelineDiagnostics.recordPersisted(context)
                val persisted = historyStore.saveEvent(historyEvent)
                if (!persisted) {
                    AppLogger.e("ProbeC persist failed riskLevel=${historyEvent.riskLevel}")
                }
            },
            notifyAlert = {
                AlertNotifier.show(
                    context = context,
                    sender = sender,
                    assessment = assessment,
                )
            },
        )
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

    private fun buildEventKey(normalizedText: String, result: RiskEngine.RiskResult): String {
        val eventText = normalizedText.take(180)
        return listOf(
            result.level.name,
            eventText,
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

    internal fun shouldNotify(
        alertType: AlertType,
        result: RiskEngine.RiskResult,
    ): Boolean {
        val strongUrlReasons =
            setOf(
                "url_shortener",
                "url_suspicious_tld",
                "url_punycode",
                "url_non_latin_hostname",
                "url_mixed_latin_cyrillic",
                "correlation_brand_url_mismatch",
                "correlation_brand_entity_mismatch",
                "keyword_dataRequest",
            )

        val lowRiskButSuspiciousUrl =
            alertType == AlertType.URL &&
                (
                    result.reasons.any { it in strongUrlReasons } ||
                        (result.score >= 20 && result.reasons.size >= 2)
                )

        return alertType == AlertType.MULTIBANCO ||
            result.level == RiskLevel.HIGH ||
            result.level == RiskLevel.MEDIUM ||
            lowRiskButSuspiciousUrl
    }

    internal fun persistAndMaybeNotify(
        event: HistoryEvent,
        shouldNotify: Boolean,
        persistEvent: (HistoryEvent) -> Unit,
        notifyAlert: () -> Boolean,
    ): Boolean {
        persistEvent(event)
        if (!shouldNotify) return false

        return runCatching {
            notifyAlert()
        }.getOrElse { error ->
            runCatching {
                AppLogger.e("ProbeC notify failed after persistence", error)
            }
            false
        }
    }
}
