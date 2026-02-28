package com.smsguard.storage

import android.content.Context
import com.smsguard.core.UrlExtractor

class TrustedDomainsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTrustedBaseDomains(): Set<String> =
        prefs.getStringSet(KEY_DOMAINS, emptySet()).orEmpty()

    fun saveTrustedBaseDomains(baseDomains: Set<String>) {
        val normalized = baseDomains.map(::normalizeHost).filter { it.isNotBlank() }.toSet()
        prefs.edit().putStringSet(KEY_DOMAINS, normalized).apply()
    }

    fun addTrustedBaseDomain(baseDomain: String) {
        val updated = getTrustedBaseDomains().toMutableSet()
        updated.add(normalizeHost(baseDomain))
        saveTrustedBaseDomains(updated)
    }

    fun isTrustedUrl(url: String): Boolean =
        isTrustedUrl(url, getTrustedBaseDomains())

    companion object {
        private const val PREFS_NAME = "trusted_domains"
        private const val KEY_DOMAINS = "base_domains"

        fun isTrustedUrl(url: String, trustedBaseDomains: Set<String>): Boolean {
            val host = UrlExtractor.extractHostTolerant(url) ?: return false
            return isTrustedHost(host, trustedBaseDomains)
        }

        fun isTrustedHost(hostRaw: String, trustedBaseDomains: Set<String>): Boolean {
            val host = normalizeHost(hostRaw)
            for (base in trustedBaseDomains) {
                val normalizedBase = normalizeHost(base)
                if (host == normalizedBase) return true
                if (host.endsWith(".$normalizedBase")) return true
            }
            return false
        }

        private fun normalizeHost(value: String): String =
            value.trim().lowercase().trimEnd('.')
    }
}
