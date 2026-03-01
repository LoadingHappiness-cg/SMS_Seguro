package com.smsguard.ui

import org.junit.Assert.assertFalse
import org.junit.Test

class SetupScreenDebugTest {

    @Test
    fun diagnosticsVisibility_isDisabledByDefault() {
        assertFalse(debugDiagnosticsEnabled(true))
        assertFalse(debugDiagnosticsEnabled(false))
    }
}
