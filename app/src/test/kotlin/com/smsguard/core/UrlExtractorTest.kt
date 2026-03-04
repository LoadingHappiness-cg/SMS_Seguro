package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlExtractorTest {

    // ---- extractUrls ----

    @Test
    fun extractUrls_httpsLink() {
        val urls = UrlExtractor.extractUrls("Visite https://example.com/path agora")
        assertEquals(listOf("https://example.com/path"), urls)
    }

    @Test
    fun extractUrls_httpLink() {
        val urls = UrlExtractor.extractUrls("Clique http://example.com")
        assertEquals(listOf("http://example.com"), urls)
    }

    @Test
    fun extractUrls_wwwWithoutScheme_prependsHttps() {
        val urls = UrlExtractor.extractUrls("Abra www.example.com/page")
        assertEquals(1, urls.size)
        assertTrue(urls[0].startsWith("https://"))
        assertTrue(urls[0].contains("www.example.com/page"))
    }

    @Test
    fun extractUrls_multipleUrls() {
        val text = "Link1: https://a.com Link2: http://b.pt/x"
        val urls = UrlExtractor.extractUrls(text)
        assertEquals(2, urls.size)
    }

    @Test
    fun extractUrls_duplicatesRemoved() {
        val text = "https://dup.com e https://dup.com"
        val urls = UrlExtractor.extractUrls(text)
        assertEquals(1, urls.size)
    }

    @Test
    fun extractUrls_noUrls_returnsEmpty() {
        val urls = UrlExtractor.extractUrls("Mensagem sem links nenhuns")
        assertTrue(urls.isEmpty())
    }

    @Test
    fun extractUrls_emptyString_returnsEmpty() {
        assertTrue(UrlExtractor.extractUrls("").isEmpty())
    }

    @Test
    fun extractUrls_urlWithQueryParams() {
        val urls = UrlExtractor.extractUrls("https://example.com/path?id=1&token=abc")
        assertEquals(1, urls.size)
        assertTrue(urls[0].contains("id=1"))
    }

    @Test
    fun extractUrls_caseInsensitive() {
        val urls = UrlExtractor.extractUrls("HTTPS://EXAMPLE.COM/PATH")
        assertEquals(1, urls.size)
    }

    @Test
    fun extractUrls_urlShortener() {
        val urls = UrlExtractor.extractUrls("Clique https://bit.ly/abc123")
        assertEquals(listOf("https://bit.ly/abc123"), urls)
    }

    // ---- getDomain ----

    @Test
    fun getDomain_stripsWww() {
        assertEquals("example.com", UrlExtractor.getDomain("https://www.example.com/path"))
    }

    @Test
    fun getDomain_noWww() {
        assertEquals("example.com", UrlExtractor.getDomain("https://example.com"))
    }

    @Test
    fun getDomain_subdomain_preserved() {
        assertEquals("sub.example.com", UrlExtractor.getDomain("https://sub.example.com"))
    }

    // ---- extractHostTolerant ----

    @Test
    fun extractHostTolerant_standardUrl() {
        assertEquals("example.com", UrlExtractor.extractHostTolerant("https://example.com/path"))
    }

    @Test
    fun extractHostTolerant_withPort() {
        val host = UrlExtractor.extractHostTolerant("https://example.com:8080/path")
        assertEquals("example.com", host)
    }

    @Test
    fun extractHostTolerant_malformedUrl_fallsBackToRegex() {
        // A URL that java.net.URL might struggle with but regex can handle
        val host = UrlExtractor.extractHostTolerant("custom://host.example.com/path")
        assertEquals("host.example.com", host)
    }

    @Test
    fun extractHostTolerant_noScheme_returnsNull() {
        assertNull(UrlExtractor.extractHostTolerant("not a url at all"))
    }

    @Test
    fun extractHostTolerant_emptyString_returnsNull() {
        assertNull(UrlExtractor.extractHostTolerant(""))
    }
}
