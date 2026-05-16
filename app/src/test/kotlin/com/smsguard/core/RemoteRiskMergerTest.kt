package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteRiskMergerTest {

    private val localResult =
        RiskEngine.RiskResult(
            score = 35,
            level = RiskLevel.LOW,
            reasons = listOf("url_present"),
            primaryUrl = "https://apoio-seguro.example/login",
            primaryDomain = "apoio-seguro.example",
            primaryBrand = "ctt",
        )

    @Test
    fun merge_withoutRemoteResult_keepsLocalAnalysisUntouched() {
        val merged =
            RemoteRiskMerger.merge(
                local = localResult,
                remote = null,
                thresholds = ScoreThresholds(medium = 40, high = 70),
            )

        assertEquals(localResult, merged.risk)
        assertNull(merged.remote)
    }

    @Test
    fun merge_withRemoteDnsBlock_elevatesRiskAndAddsReasons() {
        val remote =
            LinkEnrichmentResponse(
                dnsBlocked = true,
                dnsProvider = DnsProvider.CLOUDFLARE_FAMILIES,
                resolvedIpCount = 2,
                ipReputationScore = null,
                riskDelta = 45,
                reasons = listOf("remote_dns_blocked"),
            )

        val merged =
            RemoteRiskMerger.merge(
                local = localResult,
                remote = remote,
                thresholds = ScoreThresholds(medium = 40, high = 70),
            )

        assertEquals(80, merged.risk.score)
        assertEquals(RiskLevel.HIGH, merged.risk.level)
        assertTrue(merged.risk.reasons.contains("url_present"))
        assertTrue(merged.risk.reasons.contains("remote_dns_blocked"))
        assertEquals(remote, merged.remote)
    }

    @Test
    fun merge_capsScoreAtOneHundred() {
        val remote =
            LinkEnrichmentResponse(
                dnsBlocked = false,
                dnsProvider = DnsProvider.QUAD9,
                resolvedIpCount = 5,
                ipReputationScore = 95,
                riskDelta = 90,
                reasons = listOf("remote_ip_reputation_flagged"),
            )

        val merged =
            RemoteRiskMerger.merge(
                local = localResult.copy(score = 50, level = RiskLevel.MEDIUM),
                remote = remote,
                thresholds = ScoreThresholds(medium = 40, high = 70),
            )

        assertEquals(100, merged.risk.score)
        assertEquals(RiskLevel.HIGH, merged.risk.level)
    }
}
