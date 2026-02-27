package com.smsguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnicodeSpoofingDetectorTest {

    @Test
    fun cyrillicPaypalLookalike_detectsCyrillicAndMixed() {
        val hit = UnicodeSpoofingDetector.checkUrl("https://раураl.com/login")
        assertNotNull(hit)
        assertTrue(hit!!.hasCyrillic)
        assertTrue(hit.hasMixedLatinCyrillic)
    }

    @Test
    fun punycodeOnly_notFlaggedAsCyrillic() {
        val hit = UnicodeSpoofingDetector.checkUrl("https://xn--e1afmkfd.xn--p1ai/path")
        assertNotNull(hit)
        assertFalse(hit!!.hasCyrillic)
        assertFalse(hit.hasMixedLatinCyrillic)
    }
}
