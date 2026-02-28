package com.smsguard.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsSourceAllowlistTest {

    @Test
    fun whatsapp_isIgnored() {
        assertFalse(SmsSourceAllowlist.isAllowedPackage("com.whatsapp", defaultSmsPackage = null))
    }

    @Test
    fun whatsappBusiness_isIgnored() {
        assertFalse(SmsSourceAllowlist.isAllowedPackage("com.whatsapp.w4b", defaultSmsPackage = null))
    }

    @Test
    fun googleMessages_isAllowed() {
        assertTrue(
            SmsSourceAllowlist.isAllowedPackage(
                "com.google.android.apps.messaging",
                defaultSmsPackage = null,
            ),
        )
    }

    @Test
    fun defaultSmsPackage_isAllowedEvenIfNotInKnownList() {
        assertTrue(
            SmsSourceAllowlist.isAllowedPackage(
                "com.oem.sms",
                defaultSmsPackage = "com.oem.sms",
            ),
        )
    }
}
