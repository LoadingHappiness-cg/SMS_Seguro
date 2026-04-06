package com.smsguard.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class SetupScreenDebugTest {

    @Test
    fun diagnosticsVisibility_isDisabledByDefault() {
        assertFalse(debugDiagnosticsEnabled(true))
        assertFalse(debugDiagnosticsEnabled(false))
    }

    @Test
    fun formatRulesetLastUpdate_usesExpectedPattern() {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("01/01/1970 00:00", formatRulesetLastUpdate(0L))
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }
}
