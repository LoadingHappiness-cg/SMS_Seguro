package com.smsguard.core

class RiskEngine(private val ruleSet: RuleSet) {

    data class RiskResult(
        val score: Int,
        val level: RiskLevel,
        val reasons: List<String>,
        val primaryUrl: String,
        val primaryDomain: String,
        val primaryBrand: String?,
    )

    private val keywordGroupWeights =
        mapOf(
            "urgency" to ruleSet.scoring.keywordWeights.urgency,
            "threat" to ruleSet.scoring.keywordWeights.threat,
            "payment" to ruleSet.scoring.keywordWeights.payment,
            "dataRequest" to ruleSet.scoring.keywordWeights.dataRequest,
            "publicServices" to ruleSet.scoring.keywordWeights.publicServices,
            "delivery" to ruleSet.scoring.keywordWeights.delivery,
            "banking" to ruleSet.scoring.keywordWeights.banking,
        )

    fun analyze(
        messageText: String,
        normalizedText: String = TextNormalizer.normalize(messageText),
        urls: List<String>,
        multibancoData: MultibancoData?,
    ): RiskResult {
        var score = 0
        val reasons = linkedSetOf<String>()

        val matchedGroups = detectKeywordGroups(normalizedText)
        matchedGroups.forEach { group ->
            score += keywordGroupWeights[group] ?: 0
            reasons.add("keyword_$group")
        }

        val urlHosts = urls.map { UrlExtractor.getDomain(it).lowercase() }.filter { it.isNotBlank() }
        val primaryUrl = urls.firstOrNull().orEmpty()
        val primaryDomain = urlHosts.firstOrNull().orEmpty()

        if (urls.isNotEmpty()) {
            score += ruleSet.urlSignals.weights.hasUrl
            reasons.add("url_present")
        }

        val shortenerFound =
            urlHosts.any { host ->
                ruleSet.urlSignals.shorteners.any { shortener -> host.equals(shortener, ignoreCase = true) }
            }
        if (shortenerFound) {
            score += ruleSet.urlSignals.weights.shortener
            reasons.add("url_shortener")
        }

        val punycodeFound =
            urlHosts.any { host ->
                host.split(".").any { label -> label.startsWith("xn--") }
            }
        if (punycodeFound) {
            score += ruleSet.urlSignals.weights.punycode
            reasons.add("url_punycode")
        }

        val suspiciousTldFound =
            urlHosts.any { host ->
                val tld = host.substringAfterLast('.', "")
                ruleSet.urlSignals.suspiciousTlds.any { suspicious ->
                    tld.equals(suspicious.removePrefix("."), ignoreCase = true)
                }
            }
        if (suspiciousTldFound) {
            score += ruleSet.urlSignals.weights.suspiciousTld
            reasons.add("url_suspicious_tld")
        }

        val unicodeSignals = urls.mapNotNull { url -> UnicodeSpoofingDetector.checkUrl(url) }
        if (unicodeSignals.any { it.hasCyrillic }) {
            score += ruleSet.urlSignals.weights.cyrillicOrNonLatinHostname
            reasons.add("url_non_latin_hostname")
        }
        if (unicodeSignals.any { it.hasMixedLatinCyrillic }) {
            score += ruleSet.urlSignals.weights.mixedLatinCyrillicHostnameBonus
            reasons.add("url_mixed_latin_cyrillic")
        }

        val entityOwner = multibancoData?.entidade?.let { entity ->
            ruleSet.multibanco.entities[entity] ?: ruleSet.multibanco.intermediaries[entity]
        }
        val entityStatus =
            when {
                multibancoData == null -> "none"
                ruleSet.multibanco.entities.containsKey(multibancoData.entidade) -> "known"
                ruleSet.multibanco.intermediaries.containsKey(multibancoData.entidade) -> "intermediary"
                else -> "unknown"
            }

        if (multibancoData != null) {
            score += ruleSet.multibancoSignals.weights.hasEntityRef
            reasons.add("mb_payment_request")
            reasons.add("mb_has_entity_ref")

            if (multibancoData.amountDetected || !multibancoData.valor.isNullOrBlank()) {
                score += ruleSet.multibancoSignals.weights.hasAmount
                reasons.add("mb_has_amount")
            }

            when (entityStatus) {
                "known" -> {
                    score += ruleSet.multibancoSignals.weights.knownEntity
                    reasons.add("mb_known_entity")
                }
                "intermediary" -> {
                    score += ruleSet.multibancoSignals.weights.intermediaryEntity
                    reasons.add("mb_intermediary_entity")
                }
                "unknown" -> {
                    score += ruleSet.multibancoSignals.weights.unknownEntity
                    reasons.add("mb_unknown_entity")
                }
            }
        }

        val primaryBrand = BrandDetector.detectPrimaryBrand(normalizedText, matchedGroups)
        if (primaryBrand != null && !entityOwner.isNullOrBlank()) {
            val allowedOwners =
                ruleSet.correlation.brandEntityMap[primaryBrand].orEmpty()
            if (allowedOwners.isNotEmpty()) {
                val normalizedOwner = TextNormalizer.normalize(entityOwner)
                val matchesAllowedOwner =
                    allowedOwners.any { allowed ->
                        val normalizedAllowed = TextNormalizer.normalize(allowed)
                        normalizedOwner == normalizedAllowed || normalizedOwner.contains(normalizedAllowed)
                    }
                if (!matchesAllowedOwner) {
                    score += ruleSet.correlation.weights.brandEntityMismatch
                    reasons.add("correlation_brand_entity_mismatch")
                }
            }
        }

        if (primaryBrand != null && urlHosts.isNotEmpty()) {
            val allowedDomains = ruleSet.correlation.brandAllowedDomains[primaryBrand].orEmpty()
            if (allowedDomains.isNotEmpty()) {
                val mismatch =
                    urlHosts.all { host ->
                        allowedDomains.none { allowed ->
                            val normalizedAllowed = allowed.lowercase().removePrefix(".")
                            host == normalizedAllowed || host.endsWith(".$normalizedAllowed")
                        }
                    }
                if (mismatch) {
                    score += ruleSet.correlation.weights.brandUrlMismatch
                    reasons.add("correlation_brand_url_mismatch")
                }
            }
        }

        val mediumThreshold = ruleSet.scoring.thresholds.medium
        if (multibancoData?.entityDetected == true && multibancoData.referenceDetected && score < mediumThreshold) {
            score = mediumThreshold
            reasons.add("mb_payment_request")
        }
        if ("dataRequest" in matchedGroups && score < mediumThreshold) {
            score = mediumThreshold
            reasons.add("data_request_minimum_medium")
        }
        if (unicodeSignals.any { it.hasCyrillic } && score < mediumThreshold) {
            score = mediumThreshold
            reasons.add("non_latin_url_minimum_medium")
        }

        val finalScore = score.coerceIn(0, 100)
        val level =
            when {
                finalScore >= ruleSet.scoring.thresholds.high -> RiskLevel.HIGH
                finalScore >= ruleSet.scoring.thresholds.medium -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }

        return RiskResult(
            score = finalScore,
            level = level,
            reasons = reasons.toList(),
            primaryUrl = primaryUrl,
            primaryDomain = primaryDomain,
            primaryBrand = primaryBrand,
        )
    }

    private fun detectKeywordGroups(normalizedMessage: String): Set<String> {
        val groups = linkedSetOf<String>()

        fun matchAny(values: List<String>): Boolean =
            values.any { keyword -> normalizedMessage.contains(TextNormalizer.normalize(keyword)) }

        if (matchAny(ruleSet.keywordGroups.urgency)) groups.add("urgency")
        if (matchAny(ruleSet.keywordGroups.threat)) groups.add("threat")
        if (matchAny(ruleSet.keywordGroups.payment)) groups.add("payment")
        if (matchAny(ruleSet.keywordGroups.dataRequest)) groups.add("dataRequest")
        if (matchAny(ruleSet.keywordGroups.publicServices)) groups.add("publicServices")
        if (matchAny(ruleSet.keywordGroups.delivery)) groups.add("delivery")
        if (matchAny(ruleSet.keywordGroups.banking)) groups.add("banking")

        return groups
    }
}
