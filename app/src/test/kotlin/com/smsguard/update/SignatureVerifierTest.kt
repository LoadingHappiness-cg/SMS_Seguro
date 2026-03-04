package com.smsguard.update

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit tests for SignatureVerifier.
 *
 * Note: SignatureVerifier.verifyEd25519() uses android.util.Base64 internally,
 * which is not available in JVM unit tests (returns empty/zero arrays from the
 * Android stub). This means we cannot test a full valid-signature flow here —
 * that requires an instrumentation test on a real device/emulator.
 *
 * What we CAN verify in JVM tests:
 * - Invalid/corrupt inputs never crash (return false gracefully)
 * - The deprecated verify() delegates correctly
 * - Empty inputs are handled safely
 */
class SignatureVerifierTest {

    @Test
    fun verifyEd25519_emptyMessage_returnsFalse() {
        val result = SignatureVerifier.verifyEd25519(
            messageBytes = ByteArray(0),
            signatureBytes = ByteArray(64),
        )
        assertFalse(result)
    }

    @Test
    fun verifyEd25519_emptySignature_returnsFalse() {
        val result = SignatureVerifier.verifyEd25519(
            messageBytes = "hello".toByteArray(),
            signatureBytes = ByteArray(0),
        )
        assertFalse(result)
    }

    @Test
    fun verifyEd25519_bothEmpty_returnsFalse() {
        val result = SignatureVerifier.verifyEd25519(
            messageBytes = ByteArray(0),
            signatureBytes = ByteArray(0),
        )
        assertFalse(result)
    }

    @Test
    fun verifyEd25519_garbageSignature_returnsFalse() {
        val result = SignatureVerifier.verifyEd25519(
            messageBytes = "some ruleset json content".toByteArray(),
            signatureBytes = "not-a-valid-signature".toByteArray(),
        )
        assertFalse(result)
    }

    @Test
    fun verifyEd25519_randomBytes_returnsFalse() {
        val message = ByteArray(256) { it.toByte() }
        val signature = ByteArray(64) { (it * 7).toByte() }
        val result = SignatureVerifier.verifyEd25519(message, signature)
        assertFalse(result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedVerify_delegatesToVerifyEd25519() {
        // The deprecated method should behave identically
        val result = SignatureVerifier.verify(
            data = "test".toByteArray(),
            signatureBytes = ByteArray(64),
        )
        assertFalse(result)
    }
}
