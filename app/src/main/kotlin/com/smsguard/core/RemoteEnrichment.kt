package com.smsguard.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkEnrichmentRequest(
    val host: String,
)

@Serializable
enum class DnsProvider {
    @SerialName("cloudflare_families")
    CLOUDFLARE_FAMILIES,

    @SerialName("quad9")
    QUAD9,
}

@Serializable
data class LinkEnrichmentResponse(
    @SerialName("dns_blocked")
    val dnsBlocked: Boolean,
    @SerialName("dns_provider")
    val dnsProvider: DnsProvider,
    @SerialName("resolved_ip_count")
    val resolvedIpCount: Int,
    @SerialName("ip_reputation_score")
    val ipReputationScore: Int? = null,
    @SerialName("risk_delta")
    val riskDelta: Int,
    val reasons: List<String> = emptyList(),
)

data class RemoteRiskMergeResult(
    val risk: RiskEngine.RiskResult,
    val remote: LinkEnrichmentResponse?,
)

object RemoteRiskMerger {

    fun merge(
        local: RiskEngine.RiskResult,
        remote: LinkEnrichmentResponse?,
        thresholds: ScoreThresholds,
    ): RemoteRiskMergeResult {
        if (remote == null) {
            return RemoteRiskMergeResult(
                risk = local,
                remote = null,
            )
        }

        val mergedScore = (local.score + remote.riskDelta).coerceIn(0, 100)
        val mergedReasons = linkedSetOf<String>().apply {
            addAll(local.reasons)
            addAll(remote.reasons.filter { it.isNotBlank() })
        }

        val mergedRisk =
            local.copy(
                score = mergedScore,
                level = riskLevelFor(mergedScore, thresholds),
                reasons = mergedReasons.toList(),
            )

        return RemoteRiskMergeResult(
            risk = mergedRisk,
            remote = remote,
        )
    }

    private fun riskLevelFor(
        score: Int,
        thresholds: ScoreThresholds,
    ): RiskLevel =
        when {
            score >= thresholds.high -> RiskLevel.HIGH
            score >= thresholds.medium -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
}
