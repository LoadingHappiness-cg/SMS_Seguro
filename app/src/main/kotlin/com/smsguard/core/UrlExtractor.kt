package com.smsguard.core

import java.util.regex.Pattern

object UrlExtractor {
    private val URL_PATTERN = Pattern.compile(
        "(?:^|[\\W])((?:https?://|www\\.)[\\w\\-]+(?:\\.[\\w\\-]+)+(?:[\\w\\-.,@?^=%&:/~+#]*[\\w\\-@?^=%&/~+#])?)",
        Pattern.CASE_INSENSITIVE
    )

    fun extractUrls(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            var url = matcher.group(1) ?: continue
            
            // Normalize
            if (!url.startsWith("http", ignoreCase = true)) {
                url = "https://$url"
            }
            
            urls.add(url)
        }
        return urls.distinct()
    }

    fun getDomain(url: String): String {
        return extractHostTolerant(url).orEmpty().removePrefix("www.")
    }

    fun extractHostTolerant(url: String): String? {
        val strictUrlHost =
            runCatching { java.net.URL(url).host }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        if (strictUrlHost != null) return strictUrlHost

        val strictUriHost =
            runCatching { java.net.URI(url).host }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        if (strictUriHost != null) return strictUriHost

        val match = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://([^/\\s?#]+)").find(url)
        val hostPort = match?.groupValues?.getOrNull(1) ?: return null
        return hostPort.substringBefore(':')
    }
}
