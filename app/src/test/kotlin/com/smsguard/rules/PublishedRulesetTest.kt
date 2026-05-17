package com.smsguard.rules

import com.smsguard.core.RuleSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PublishedRulesetTest {

    @Test
    fun publishedRuleset_includesFnacBrandDomainCorrelation() {
        val file = sequenceOf(
            File("rules/ruleset-latest.json"),
            File("../rules/ruleset-latest.json"),
        ).firstOrNull(File::exists) ?: File("rules/ruleset-latest.json")
        assertTrue("ruleset-latest.json should exist", file.exists())

        val ruleSet = RuleLoader.json.decodeFromString(RuleSet.serializer(), file.readText())

        assertEquals(listOf("fnac.pt"), ruleSet.correlation.brandAllowedDomains["fnac"])
        assertEquals(listOf("dhl.com", "dhl.pt"), ruleSet.correlation.brandAllowedDomains["dhl"])
        assertEquals(listOf("ups.com"), ruleSet.correlation.brandAllowedDomains["ups"])
        assertEquals(listOf("dpd.pt", "dpd.com"), ruleSet.correlation.brandAllowedDomains["dpd"])
        assertEquals(9, ruleSet.version)
    }
}
