package com.smsguard.core

import com.smsguard.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class RemoteEnrichmentConfig(
    val enabled: Boolean,
    val allowedInBuild: Boolean,
    val baseUrl: String,
    val timeoutMs: Long = 1_500L,
    val traceEnabled: Boolean = false,
) {
    val isActive: Boolean
        get() = allowedInBuild && enabled && baseUrl.isNotBlank()

    val shouldTrace: Boolean
        get() = allowedInBuild && enabled && traceEnabled

    companion object {
        fun fromBuildConfig(): RemoteEnrichmentConfig =
            RemoteEnrichmentConfig(
                enabled = BuildConfig.REMOTE_ENRICHMENT_ENABLED,
                allowedInBuild = BuildConfig.REMOTE_ENRICHMENT_ALLOWED,
                baseUrl = BuildConfig.REMOTE_ENRICHMENT_BASE_URL.trim(),
                timeoutMs = BuildConfig.REMOTE_ENRICHMENT_TIMEOUT_MS.toLong(),
                traceEnabled = BuildConfig.REMOTE_ENRICHMENT_TRACE,
            )
    }
}

interface LinkEnrichmentClient {
    fun enrich(host: String): LinkEnrichmentResponse
}

object RemoteEnrichmentHostValidator {

    fun normalize(host: String): String? {
        if (host.isBlank() || host != host.trim() || host != host.lowercase()) return null

        val trimmed = host.trim().lowercase()
        if (trimmed.isBlank() || trimmed.length > 253 || trimmed.endsWith('.')) return null
        if (InetAddressUtils.isIpLiteral(trimmed)) return null
        if (trimmed.any { it == '/' || it == ':' || it == '?' || it == '#' || it == '@' }) return null

        val labels = trimmed.split(".")
        if (labels.size < 2) return null

        val validLabels =
            labels.all { label ->
                label.length in 1..63 &&
                    label.first().isAsciiLetterOrDigit() &&
                    label.last().isAsciiLetterOrDigit() &&
                    label.all { it.isAsciiLetterOrDigit() || it == '-' }
            }

        return trimmed.takeIf { validLabels }
    }

    private fun Char.isAsciiLetterOrDigit(): Boolean =
        this in 'a'..'z' || this in '0'..'9'
}

private object InetAddressUtils {
    fun isIpLiteral(value: String): Boolean =
        IPV4_REGEX.matches(value) || value.contains(':')

    private val IPV4_REGEX =
        Regex("""^(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}$""")
}

class HttpLinkEnrichmentClient(
    private val config: RemoteEnrichmentConfig,
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .callTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
            .connectTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
            .build(),
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
        },
) : LinkEnrichmentClient {

    override fun enrich(host: String): LinkEnrichmentResponse {
        val endpoint = config.baseUrl.trimEnd('/') + "/api/link-enrich"
        val requestBody =
            json.encodeToString(LinkEnrichmentRequest.serializer(), LinkEnrichmentRequest(host))
                .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request =
            Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Remote enrichment HTTP ${response.code}")
            }

            val body = response.body?.string() ?: throw java.io.IOException("Remote enrichment body missing")
            return json.decodeFromString(LinkEnrichmentResponse.serializer(), body)
        }
    }
}

class RemoteEnrichmentCoordinator(
    private val config: RemoteEnrichmentConfig,
    private val client: LinkEnrichmentClient,
    private val traceSink: (String) -> Unit = { message -> AppLogger.d(message) },
) {

    fun enrich(
        local: RiskEngine.RiskResult,
        thresholds: ScoreThresholds,
    ): RemoteRiskMergeResult {
        if (!config.isActive) {
            trace(
                "RemoteEnrichment skip inactive " +
                    "allowed=${config.allowedInBuild} enabled=${config.enabled} " +
                    "baseUrlPresent=${config.baseUrl.isNotBlank()}",
            )
            return RemoteRiskMerger.merge(local, remote = null, thresholds = thresholds)
        }

        val host = RemoteEnrichmentHostValidator.normalize(local.primaryDomain)
            ?: run {
                trace("RemoteEnrichment skip invalid_host value=${local.primaryDomain}")
                return RemoteRiskMerger.merge(local, remote = null, thresholds = thresholds)
            }

        return runCatching {
            val remote = client.enrich(host)
            trace(
                "RemoteEnrichment success host=$host dnsBlocked=${remote.dnsBlocked} " +
                    "delta=${remote.riskDelta} reasons=${remote.reasons.joinToString(",")}",
            )
            RemoteRiskMerger.merge(local, remote = remote, thresholds = thresholds)
        }.getOrElse {
            trace("RemoteEnrichment fail_open host=$host error=${it::class.simpleName}")
            RemoteRiskMerger.merge(local, remote = null, thresholds = thresholds)
        }
    }

    private fun trace(message: String) {
        if (config.shouldTrace) {
            traceSink(message)
        }
    }
}
