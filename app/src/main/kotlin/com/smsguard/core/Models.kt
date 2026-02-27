package com.smsguard.core

import kotlinx.serialization.Serializable

@Serializable
enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

@Serializable
data class HistoryEvent(
    val timestamp: Long,
    val sender: String,
    val domain: String = "",
    val url: String? = null,
    val alertType: AlertType = AlertType.URL,
    val multibancoEntidade: String? = null,
    val multibancoReferencia: String? = null,
    val multibancoValor: String? = null,
    val score: Int,
    val riskLevel: RiskLevel,
    val reasons: List<String> = emptyList()
)

@Serializable
data class RuleSet(
    val version: Int,
    val publishedAt: String,
    val scoring: ScoringConfig = ScoringConfig(),
    val keywordGroups: KeywordGroups = KeywordGroups(),
    val urlSignals: UrlSignals = UrlSignals(),
    val multibancoSignals: MultibancoSignals = MultibancoSignals(),
    val correlation: CorrelationConfig = CorrelationConfig(),
    val multibanco: MultibancoConfig = MultibancoConfig(),
)

@Serializable
data class ScoringConfig(
    val thresholds: ScoreThresholds = ScoreThresholds(),
)

@Serializable
data class ScoreThresholds(
    val medium: Int = 40,
    val high: Int = 70,
)

@Serializable
data class KeywordGroups(
    val urgency: List<String> = emptyList(),
    val threat: List<String> = emptyList(),
    val payment: List<String> = emptyList(),
    val dataRequest: List<String> = emptyList(),
    val publicServices: List<String> = emptyList(),
    val delivery: List<String> = emptyList(),
    val banking: List<String> = emptyList(),
)

@Serializable
data class UrlSignals(
    val suspiciousTlds: List<String> = emptyList(),
    val shorteners: List<String> = emptyList(),
    val weights: UrlWeights = UrlWeights(),
)

@Serializable
data class UrlWeights(
    val hasUrl: Int = 10,
    val shortener: Int = 20,
    val punycode: Int = 35,
    val cyrillicOrNonLatinHostname: Int = 50,
    val mixedLatinCyrillicHostnameBonus: Int = 20,
    val suspiciousTld: Int = 25,
)

@Serializable
data class MultibancoSignals(
    val weights: MultibancoWeights = MultibancoWeights(),
)

@Serializable
data class MultibancoWeights(
    val hasEntityRef: Int = 25,
    val hasAmount: Int = 10,
    val unknownEntity: Int = 30,
    val intermediaryEntity: Int = 20,
    val knownEntity: Int = -10,
)

@Serializable
data class CorrelationConfig(
    val weights: CorrelationWeights = CorrelationWeights(),
    val brandEntityMap: Map<String, List<String>> = emptyMap(),
    val brandAllowedDomains: Map<String, List<String>> = emptyMap(),
)

@Serializable
data class CorrelationWeights(
    val brandEntityMismatch: Int = 50,
    val brandUrlMismatch: Int = 35,
)

@Serializable
data class MultibancoConfig(
    val entities: Map<String, String> = emptyMap(),
    val intermediaries: Map<String, String> = emptyMap(),
    val criticalBrands: List<String> = emptyList(),
)
