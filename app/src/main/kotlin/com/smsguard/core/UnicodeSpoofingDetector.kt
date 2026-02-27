package com.smsguard.core

object UnicodeSpoofingDetector {

    data class SpoofingHit(
        val hasCyrillic: Boolean,
        val hasMixedLatinCyrillic: Boolean,
    )

    fun checkUrl(url: String): SpoofingHit? {
        val host = extractHostTolerant(url)?.lowercase()?.removePrefix("www.") ?: return null
        if (host.isBlank()) return null

        val hasCyrillic = Regex("[\\u0400-\\u04FF]").containsMatchIn(host)
        val hasLatin = Regex("[A-Za-z]").containsMatchIn(host)

        return SpoofingHit(
            hasCyrillic = hasCyrillic,
            hasMixedLatinCyrillic = hasLatin && hasCyrillic,
        )
    }

    private fun extractHostTolerant(url: String): String? {
        try {
            val strict = java.net.URL(url).host
            if (!strict.isNullOrBlank()) return strict
        } catch (_: Exception) {
        }

        val match = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/\\s?#]+)").find(url)
        val hostPort = match?.groupValues?.getOrNull(1) ?: return null
        return hostPort.substringBefore(':')
    }
}
