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
        return try {
            val uri = java.net.URI(url)
            val domain = uri.host ?: ""
            domain.removePrefix("www.")
        } catch (e: Exception) {
            ""
        }
    }
}
