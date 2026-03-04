package com.smsguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpDetectorTest {

    @Test
    fun otpOnly_isSafe() {
        // "246747 é o seu código de verificação do Link" normalizado
        val text = TextNormalizer.normalize("246747 e o seu codigo de verificacao do Link")
        assertTrue(OtpDetector.isSafeOtp(text, emptyList()))
    }

    @Test
    fun otpWithMaliciousUrl_isNotSafe() {
        val text = TextNormalizer.normalize("246747 e o seu codigo de verificacao")
        val urls = listOf("https://micros0ft-login.xyz")
        assertFalse(OtpDetector.isSafeOtp(text, urls))
    }

    @Test
    fun otpWithPaymentThreat_isNotSafe() {
        val text = TextNormalizer.normalize("246747 codigo de verificacao — multa pendente penhora urgente")
        assertFalse(OtpDetector.isSafeOtp(text, emptyList()))
    }

    @Test
    fun otpWithSensitiveDataRequest_isNotSafe() {
        val text = TextNormalizer.normalize("246747 codigo de verificacao — introduza o seu iban")
        assertFalse(OtpDetector.isSafeOtp(text, emptyList()))
    }

    @Test
    fun noOtpCode_isNotSafe() {
        val text = TextNormalizer.normalize("confirme a sua conta clicando no link")
        assertFalse(OtpDetector.isSafeOtp(text, emptyList()))
    }

    @Test
    fun noOtpKeyword_isNotSafe() {
        val text = TextNormalizer.normalize("O seu saldo e de 246747 euros")
        assertFalse(OtpDetector.isSafeOtp(text, emptyList()))
    }
}
