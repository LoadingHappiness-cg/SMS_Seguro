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
        val domainFromUri =
            runCatching {
                java.net.URI(url).host
            }.getOrNull()

        val domainFromUrl =
            if (domainFromUri.isNullOrBlank()) {
                runCatching { java.net.URL(url).host }.getOrNull()
            } else {
                null
            }

        val domain = (domainFromUri ?: domainFromUrl).orEmpty()
        return domain.removePrefix("www.")
    }
}
