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
    val baseUrl: String,
    val timeoutMs: Long = 1_500L,
) {
    val isActive: Boolean
        get() = enabled && baseUrl.isNotBlank()

    companion object {
        fun fromBuildConfig(): RemoteEnrichmentConfig =
            RemoteEnrichmentConfig(
                enabled = BuildConfig.REMOTE_ENRICHMENT_ENABLED,
                baseUrl = BuildConfig.REMOTE_ENRICHMENT_BASE_URL.trim(),
                timeoutMs = BuildConfig.REMOTE_ENRICHMENT_TIMEOUT_MS.toLong(),
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
) {

    fun enrich(
        local: RiskEngine.RiskResult,
        thresholds: ScoreThresholds,
    ): RemoteRiskMergeResult {
        if (!config.isActive) {
            return RemoteRiskMerger.merge(local, remote = null, thresholds = thresholds)
        }

        val host = RemoteEnrichmentHostValidator.normalize(local.primaryDomain)
            ?: return RemoteRiskMerger.merge(local, remote = null, thresholds = thresholds)

        return runCatching {
            val remote = client.enrich(host)
            RemoteRiskMerger.merge(local, remote = remote, thresholds = thresholds)
        }.getOrElse {
            RemoteRiskMerger.merge(local, remote = null, thresholds = thresholds)
        }
    }
}
