package com.smsguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedAuthFlowDetectorTest {

    @Test
    fun trustedGoogleOAuthFlow_isRecognizedAsSafe() {
        val urls = listOf("https://accounts.google.com/o/oauth2/v2/auth?client_id=abc&redirect_uri=smsseguro://protecao&code=1234")

        assertTrue(
            TrustedAuthFlowDetector.isTrustedAuthFlow(
                normalizedText = TextNormalizer.normalize("Use este link para concluir o login na sua conta Google."),
                urls = urls,
            ),
        )
    }

    @Test
    fun trustedHostWithoutOAuthParams_isNotRecognizedAsSafe() {
        val urls = listOf("https://accounts.google.com/ServiceLogin")

        assertFalse(
            TrustedAuthFlowDetector.isTrustedAuthFlow(
                normalizedText = TextNormalizer.normalize("Confirme o acesso à sua conta."),
                urls = urls,
            ),
        )
    }

    @Test
    fun untrustedOAuthLikeLink_isNotRecognizedAsSafe() {
        val urls = listOf("https://accounts-google-login.xyz/auth?client_id=abc&redirect_uri=smsseguro://protecao&code=1234")

        assertFalse(
            TrustedAuthFlowDetector.isTrustedAuthFlow(
                normalizedText = TextNormalizer.normalize("Use este link para concluir o login."),
                urls = urls,
            ),
        )
    }
}
