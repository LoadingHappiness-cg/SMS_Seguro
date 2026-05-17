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
        sender: String? = null,
        urls: List<String>,
        multibancoData: MultibancoData?,
    ): RiskResult {
        var score = 0
        val reasons = linkedSetOf<String>()
        val normalizedSender = sender?.let { TextNormalizer.normalize(it) }.orEmpty()

        val matchedMessageRules = ruleSet.messageRules.filter { ruleMatches(it, normalizedText) }
        val suppressDataRequestStacking =
            matchedMessageRules.any { rule -> rule.id == "bank_withdrawal_callback_scam" }

        val matchedGroups = detectKeywordGroups(normalizedText)
        matchedGroups.forEach { group ->
            if (group == "dataRequest" && suppressDataRequestStacking) return@forEach
            score += keywordGroupWeights[group] ?: 0
            reasons.add("keyword_$group")
        }

        matchedMessageRules.forEach { rule ->
            score += rule.score
            rule.reasons.filter { it.isNotBlank() }.forEach(reasons::add)

            if (rule.brandAnyContains.isNotEmpty() &&
                rule.brandBonusScore != 0 &&
                containsAny(normalizedText, normalizedSender, rule.brandAnyContains)
            ) {
                score += rule.brandBonusScore
                reasons.add(rule.brandBonusReason.ifBlank { "Pode estar a imitar uma entidade bancária" })
            }
        }

        val urlHosts = urls.map { UrlExtractor.getDomain(it).lowercase() }.filter { it.isNotBlank() }
        val primaryUrl = urls.firstOrNull().orEmpty()
        val primaryDomain = urlHosts.firstOrNull().orEmpty()
        val hasLoginPath =
            urls.any { url ->
                url.contains("/login", ignoreCase = true) || url.contains("/signin", ignoreCase = true)
            }
        val hasAccountAccessContext =
            containsAny(
                normalizedText = normalizedText,
                normalizedSender = normalizedSender,
                tokens = listOf("acesso", "login", "iniciar sessao", "sessao", "conta"),
            )

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

        val brandMismatchEscalationContext =
            "delivery" in matchedGroups ||
                "payment" in matchedGroups ||
                "banking" in matchedGroups ||
                "dataRequest" in matchedGroups ||
                hasAccountAccessContext
        if ("correlation_brand_url_mismatch" in reasons && brandMismatchEscalationContext) {
            score += 20
            reasons.add("correlation_brand_url_mismatch_context")
        }

        val senderBrandMismatchContext =
            "correlation_brand_url_mismatch" in reasons &&
                brandMismatchEscalationContext &&
                isGenericOrNonInstitutionalSender(normalizedSender) &&
                primaryBrand?.let { brand ->
                    !senderAppearsRelatedToBrand(normalizedSender, brand)
                } == true
        if (senderBrandMismatchContext) {
            score += 10
            reasons.add("correlation_sender_brand_mismatch_context")
        }

        val suspiciousLoginEscalationContext =
            ("urgency" in matchedGroups || "threat" in matchedGroups || "payment" in matchedGroups) &&
                (hasAccountAccessContext || hasLoginPath)
        if ("url_suspicious_tld" in reasons && suspiciousLoginEscalationContext) {
            score += 20
            reasons.add("url_suspicious_tld_context")
        }

        val mediumThreshold = ruleSet.scoring.thresholds.medium
        if (multibancoData?.entityDetected == true && multibancoData.referenceDetected && score < mediumThreshold) {
            score = mediumThreshold
            reasons.add("mb_payment_request")
        }
        if ("dataRequest" in matchedGroups && !suppressDataRequestStacking && score < mediumThreshold) {
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

    private fun ruleMatches(rule: MessageRule, normalizedText: String): Boolean {
        if (rule.allOfContains.any { token -> !normalizedText.contains(TextNormalizer.normalize(token)) }) {
            return false
        }

        if (rule.regexAny.isNotEmpty()) {
            val anyRegexMatches =
                rule.regexAny.any { pattern ->
                    runCatching {
                        Regex(pattern).containsMatchIn(normalizedText)
                    }.getOrElse { false }
                }
            if (!anyRegexMatches) return false
        }

        return true
    }

    private fun containsAny(
        normalizedText: String,
        normalizedSender: String,
        tokens: List<String>,
    ): Boolean {
        if (tokens.isEmpty()) return false

        return tokens.any { token ->
            val normalizedToken = TextNormalizer.normalize(token)
            normalizedText.contains(normalizedToken) || normalizedSender.contains(normalizedToken)
        }
    }

    private fun detectKeywordGroups(normalizedMessage: String): Set<String> {
        val groups = linkedSetOf<String>()

        fun matchAny(group: String, values: List<String>): Boolean =
            values.any { keyword ->
                val normalizedKeyword = TextNormalizer.normalize(keyword)
                when {
                    group == "urgency" && normalizedKeyword == "ate" ->
                        normalizedMessage.contains(normalizedKeyword) && !containsPromotionalAteDatePhrase(normalizedMessage)
                    else -> normalizedMessage.contains(normalizedKeyword)
                }
            }

        if (matchAny("urgency", ruleSet.keywordGroups.urgency)) groups.add("urgency")
        if (matchAny("threat", ruleSet.keywordGroups.threat)) groups.add("threat")
        if (matchAny("payment", ruleSet.keywordGroups.payment)) groups.add("payment")
        if (matchAny("dataRequest", ruleSet.keywordGroups.dataRequest)) groups.add("dataRequest")
        if (matchAny("publicServices", ruleSet.keywordGroups.publicServices)) groups.add("publicServices")
        if (matchAny("delivery", ruleSet.keywordGroups.delivery)) groups.add("delivery")
        if (matchAny("banking", ruleSet.keywordGroups.banking)) groups.add("banking")

        return groups
    }

    private fun containsPromotionalAteDatePhrase(normalizedMessage: String): Boolean =
        Regex("""\bate\s+\d{1,2}/\d{1,2}\b""").containsMatchIn(normalizedMessage)

    private fun senderAppearsRelatedToBrand(
        normalizedSender: String,
        brand: String,
    ): Boolean {
        if (normalizedSender.isBlank()) return false
        val compactSender = normalizedSender.filter(Char::isLetterOrDigit)
        val compactBrand = brand.filter(Char::isLetterOrDigit)
        return compactSender.contains(compactBrand) || compactBrand.contains(compactSender)
    }

    private fun isGenericOrNonInstitutionalSender(normalizedSender: String): Boolean {
        if (normalizedSender.isBlank()) return false
        if (normalizedSender.any(Char::isDigit)) return true
        if (normalizedSender.contains(' ')) return true
        return normalizedSender.all { it.isLetter() } && normalizedSender.length in 3..16
    }
}
