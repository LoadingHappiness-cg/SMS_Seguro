package com.smsguard.notification

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.smsguard.core.AppLogger
import com.smsguard.core.RiskLevel

data class AlertPipelineSnapshot(
    val lastEventAt: Long = 0L,
    val lastEventSource: String = "",
    val lastEventPackage: String = "",
    val lastRiskAt: Long = 0L,
    val lastRiskLevel: String = "",
    val lastRiskScore: Int? = null,
    val lastAlertPersistedAt: Long = 0L,
    val lastAlertNotifiedAt: Long = 0L,
)

object AlertPipelineDiagnostics {

    private const val PREFS_NAME = "alert_pipeline_diagnostics"
    private const val KEY_LAST_EVENT_AT = "last_event_at"
    private const val KEY_LAST_EVENT_SOURCE = "last_event_source"
    private const val KEY_LAST_EVENT_PACKAGE = "last_event_package"
    private const val KEY_LAST_RISK_AT = "last_risk_at"
    private const val KEY_LAST_RISK_LEVEL = "last_risk_level"
    private const val KEY_LAST_RISK_SCORE = "last_risk_score"
    private const val KEY_LAST_ALERT_PERSISTED_AT = "last_alert_persisted_at"
    private const val KEY_LAST_ALERT_NOTIFIED_AT = "last_alert_notified_at"

    fun snapshot(context: Context): AlertPipelineSnapshot {
        if (!AppLogger.isDebugEnabled) return AlertPipelineSnapshot()
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val storedScore = prefs.getInt(KEY_LAST_RISK_SCORE, -1)
        return AlertPipelineSnapshot(
            lastEventAt = prefs.getLong(KEY_LAST_EVENT_AT, 0L),
            lastEventSource = prefs.getString(KEY_LAST_EVENT_SOURCE, "").orEmpty(),
            lastEventPackage = prefs.getString(KEY_LAST_EVENT_PACKAGE, "").orEmpty(),
            lastRiskAt = prefs.getLong(KEY_LAST_RISK_AT, 0L),
            lastRiskLevel = prefs.getString(KEY_LAST_RISK_LEVEL, "").orEmpty(),
            lastRiskScore = storedScore.takeIf { it >= 0 },
            lastAlertPersistedAt = prefs.getLong(KEY_LAST_ALERT_PERSISTED_AT, 0L),
            lastAlertNotifiedAt = prefs.getLong(KEY_LAST_ALERT_NOTIFIED_AT, 0L),
        )
    }

    fun recordEvent(
        context: Context,
        source: String,
        packageName: String?,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        if (!AppLogger.isDebugEnabled) return
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_EVENT_AT, timestamp)
            .putString(KEY_LAST_EVENT_SOURCE, source)
            .putString(KEY_LAST_EVENT_PACKAGE, packageName.orEmpty())
            .apply()
    }

    fun recordRisk(
        context: Context,
        level: RiskLevel,
        score: Int,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        if (!AppLogger.isDebugEnabled) return
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_RISK_AT, timestamp)
            .putString(KEY_LAST_RISK_LEVEL, level.name)
            .putInt(KEY_LAST_RISK_SCORE, score)
            .apply()
    }

    fun recordPersisted(
        context: Context,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        if (!AppLogger.isDebugEnabled) return
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_ALERT_PERSISTED_AT, timestamp)
            .apply()
    }

    fun recordNotified(
        context: Context,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        if (!AppLogger.isDebugEnabled) return
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_ALERT_NOTIFIED_AT, timestamp)
            .apply()
    }
}
