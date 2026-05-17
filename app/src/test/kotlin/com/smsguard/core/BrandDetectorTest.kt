package com.smsguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BrandDetectorTest {

    @Test
    fun detectPrimaryBrand_recognizesFnacExplicitly() {
        val detected =
            BrandDetector.detectPrimaryBrand(
                normalizedMessage = "veja os detalhes da sua encomenda fnac em: https://example.com/info?id=2",
                matchedGroups = setOf("delivery"),
            )

        assertEquals("fnac", detected)
    }
}
