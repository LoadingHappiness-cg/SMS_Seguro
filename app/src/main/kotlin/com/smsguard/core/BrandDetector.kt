package com.smsguard.core

object BrandDetector {

    private val explicitBrandKeywords =
        mapOf(
            "ctt" to listOf("ctt"),
            "dhl" to listOf("dhl"),
            "ups" to listOf("ups"),
            "dpd" to listOf("dpd"),
            "financas" to listOf("financas", "autoridade tributaria", "at"),
            "seguranca social" to listOf("seguranca social"),
            "sns" to listOf("sns", "sns24"),
            "edp" to listOf("edp"),
            "meo" to listOf("meo"),
            "vodafone" to listOf("vodafone"),
            "nos" to listOf("nos"),
            "mbway" to listOf("mbway"),
        )

    fun detectPrimaryBrand(
        normalizedMessage: String,
        matchedGroups: Set<String>,
    ): String? {
        explicitBrandKeywords.forEach { (brand, keywords) ->
            if (keywords.any { normalizedMessage.contains(it) }) {
                return brand
            }
        }

        return when {
            "delivery" in matchedGroups -> "ctt"
            "publicServices" in matchedGroups -> "financas"
            "banking" in matchedGroups -> "banking"
            else -> null
        }
    }
}
