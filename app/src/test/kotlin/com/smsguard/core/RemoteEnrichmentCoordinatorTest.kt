package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteEnrichmentCoordinatorTest {

    private val thresholds = ScoreThresholds(medium = 40, high = 70)
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
    fun featureFlagOff_skipsRemoteEnrichment() {
        var calls = 0
        val coordinator =
            RemoteEnrichmentCoordinator(
                config = RemoteEnrichmentConfig(enabled = false, baseUrl = "https://backend.example"),
                client = object : LinkEnrichmentClient {
                    override fun enrich(host: String): LinkEnrichmentResponse {
                        calls += 1
                        error("Should not be called")
                    }
                },
            )

        val merged = coordinator.enrich(localResult, thresholds)

        assertEquals(0, calls)
        assertEquals(localResult, merged.risk)
        assertNull(merged.remote)
    }

    @Test
    fun timeoutOrFailure_keepsLocalResult() {
        val coordinator =
            RemoteEnrichmentCoordinator(
                config = RemoteEnrichmentConfig(enabled = true, baseUrl = "https://backend.example"),
                client = object : LinkEnrichmentClient {
                    override fun enrich(host: String): LinkEnrichmentResponse {
                        throw java.net.SocketTimeoutException("timed out")
                    }
                },
            )

        val merged = coordinator.enrich(localResult, thresholds)

        assertEquals(localResult, merged.risk)
        assertNull(merged.remote)
    }

    @Test
    fun invalidHost_skipsRemoteEnrichment() {
        var calls = 0
        val coordinator =
            RemoteEnrichmentCoordinator(
                config = RemoteEnrichmentConfig(enabled = true, baseUrl = "https://backend.example"),
                client = object : LinkEnrichmentClient {
                    override fun enrich(host: String): LinkEnrichmentResponse {
                        calls += 1
                        error("Should not be called")
                    }
                },
            )

        val merged = coordinator.enrich(localResult.copy(primaryDomain = "https://bad.example/path"), thresholds)

        assertEquals(0, calls)
        assertEquals(localResult.copy(primaryDomain = "https://bad.example/path"), merged.risk)
        assertNull(merged.remote)
    }

    @Test
    fun successfulEnrichment_mergesRemoteSignalsIntoLocalResult() {
        val coordinator =
            RemoteEnrichmentCoordinator(
                config = RemoteEnrichmentConfig(enabled = true, baseUrl = "https://backend.example"),
                client = object : LinkEnrichmentClient {
                    override fun enrich(host: String): LinkEnrichmentResponse {
                        assertEquals("apoio-seguro.example", host)
                        return LinkEnrichmentResponse(
                            dnsBlocked = true,
                            dnsProvider = DnsProvider.QUAD9,
                            resolvedIpCount = 0,
                            ipReputationScore = null,
                            riskDelta = 45,
                            reasons = listOf("remote_dns_blocked"),
                        )
                    }
                },
            )

        val merged = coordinator.enrich(localResult, thresholds)

        assertEquals(RiskLevel.HIGH, merged.risk.level)
        assertEquals(80, merged.risk.score)
        assertTrue(merged.risk.reasons.contains("remote_dns_blocked"))
        assertEquals(DnsProvider.QUAD9, merged.remote?.dnsProvider)
    }
}
