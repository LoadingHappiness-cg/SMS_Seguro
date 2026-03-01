package com.smsguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiSupportTest {

    @Test
    fun manufacturerDetection_matchesXiaomiBrands() {
        assertTrue(isXiaomiManufacturer("Xiaomi"))
        assertTrue(isXiaomiManufacturer("REDMI"))
        assertTrue(isXiaomiManufacturer("Poco"))
        assertFalse(isXiaomiManufacturer("Samsung"))
    }

    @Test
    fun miuiDetection_matchesKnownProperties() {
        assertTrue(
            hasMiuiOrHyperOsProperties(
                mapOf("ro.miui.ui.version.name" to "V14"),
            ),
        )
        assertTrue(
            hasMiuiOrHyperOsProperties(
                mapOf("ro.miui.ui.version.incremental" to "OS1.0.3.0"),
            ),
        )
        assertFalse(hasMiuiOrHyperOsProperties(emptyMap()))
    }
}
