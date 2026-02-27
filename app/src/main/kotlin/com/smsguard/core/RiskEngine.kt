package com.smsguard.core

import java.util.Locale

class RiskEngine(private val ruleSet: RuleSet) {

    data class RiskResult(
        val score: Int,
        val level: RiskLevel,
        val reasons: List<String>
    )

    fun analyze(url: String, messageText: String): RiskResult {
        val domain = UrlExtractor.getDomain(url).lowercase(Locale.ROOT)
        val text = messageText.lowercase(Locale.ROOT)
        
        var score = 0
        val reasons = mutableListOf<String>()

        // 1. Allowlist
        if (ruleSet.allowlistDomains.any { domain == it || domain.endsWith(".$it") }) {
            score += ruleSet.weights["allowlist"] ?: -20
            reasons.add("safe_domain")
        }

        // 2. Shorteners
        if (ruleSet.shorteners.contains(domain)) {
            score += ruleSet.weights["shortener"] ?: 40
            reasons.add("shortener")
        }

        // 3. Suspicious TLDs
        if (ruleSet.suspiciousTlds.any { domain.endsWith(it) }) {
            score += ruleSet.weights["suspiciousTld"] ?: 30
            reasons.add("suspicious_tld")
        }

        // 4. Punycode (Internationalized Domain Names)
        if (domain.startsWith("xn--")) {
            score += ruleSet.weights["punycode"] ?: 25
            reasons.add("punycode")
        }

        // 5. Trigger Words
        if (ruleSet.triggerWordsPt.any { text.contains(it) }) {
            score += ruleSet.weights["triggerWords"] ?: 20
            reasons.add("trigger_words")
        }

        // 6. Brand Impersonation
        if (ruleSet.brandKeywordsPt.any { domain.contains(it) && !ruleSet.allowlistDomains.contains(domain) }) {
            score += ruleSet.weights["brandImpersonation"] ?: 20
            reasons.add("brand_impersonation")
        }

        // 7. Weird Structure (Too many dots, numbers in domain)
        if (domain.count { it == '.' } > 3 || domain.any { it.isDigit() }) {
            score += ruleSet.weights["weirdStructure"] ?: 15
            reasons.add("weird_structure")
        }

        // Clamp score
        val finalScore = score.coerceIn(0, 100)
        val level = when {
            finalScore >= 70 -> RiskLevel.HIGH
            finalScore >= 40 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return RiskResult(finalScore, level, reasons)
    }
}
