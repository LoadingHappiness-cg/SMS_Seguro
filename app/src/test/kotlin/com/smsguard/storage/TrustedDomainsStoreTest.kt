package com.smsguard.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedDomainsStoreTest {

    private val trusted = setOf("amnistia.pt")

    @Test
    fun exactBaseDomain_isTrusted() {
        assertTrue(TrustedDomainsStore.isTrustedHost("amnistia.pt", trusted))
    }

    @Test
    fun wwwSubdomain_isTrusted() {
        assertTrue(TrustedDomainsStore.isTrustedHost("www.amnistia.pt", trusted))
    }

    @Test
    fun nestedSubdomain_isTrusted() {
        assertTrue(TrustedDomainsStore.isTrustedHost("doar.amnistia.pt", trusted))
    }

    @Test
    fun suffixTrick_isNotTrusted() {
        assertFalse(TrustedDomainsStore.isTrustedHost("amnistia.pt.evil.com", trusted))
    }

    @Test
    fun prefixTrick_isNotTrusted() {
        assertFalse(TrustedDomainsStore.isTrustedHost("evilamnistia.pt", trusted))
    }

    @Test
    fun urlMatching_usesTolerantHostExtraction() {
        assertTrue(TrustedDomainsStore.isTrustedUrl("https://www.amnistia.pt/doar", trusted))
        assertFalse(TrustedDomainsStore.isTrustedUrl("https://amnistia.pt.evil.com/doar", trusted))
    }
}
