package com.smsguard.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant

class SetupScreenDebugTest {

    @Test
    fun diagnosticsVisibility_isDisabledByDefault() {
        assertFalse(debugDiagnosticsEnabled(true))
        assertFalse(debugDiagnosticsEnabled(false))
    }

    @Test
    fun rulesetTimestampFormatting_usesPortugalStyle() {
        val timestamp = Instant.parse("2026-01-15T09:05:00Z").toEpochMilli()

        assertEquals("15/01/2026 09:05", formatRulesetTimestamp(timestamp))
    }

    @Test
    fun rulesetPublishedAtFormatting_parsesIsoAndFallsBackForBlank() {
        assertEquals("01/04/2026 12:51", formatRulesetPublishedAt("2026-04-01T11:51:30Z"))
        assertEquals("Por verificar", formatRulesetPublishedAt(""))
    }
}
