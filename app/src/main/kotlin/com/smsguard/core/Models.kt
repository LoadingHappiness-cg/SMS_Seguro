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
    val domain: String,
    val score: Int,
    val riskLevel: RiskLevel,
    val reasons: List<String> = emptyList()
)

@Serializable
data class RuleSet(
    val version: Int,
    val publishedAt: String,
    val shorteners: List<String>,
    val suspiciousTlds: List<String>,
    val triggerWordsPt: List<String>,
    val brandKeywordsPt: List<String>,
    val allowlistDomains: List<String>,
    val weights: Map<String, Int>
)
