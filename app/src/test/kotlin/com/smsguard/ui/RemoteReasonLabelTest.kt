package com.smsguard.ui

import com.smsguard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RemoteReasonLabelTest {

    @Test
    fun remoteDnsBlockedReason_mapsToLocalizedString() {
        val text =
            reasonLabelText(
                code = "remote_dns_blocked",
                resolve = { resId ->
                    assertEquals(R.string.reason_remote_dns_blocked, resId)
                    "O destino do link apresenta sinais externos de risco."
                },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("O destino do link apresenta sinais externos de risco.", text)
        assertFalse(text.contains("remote_dns_blocked"))
    }

    @Test
    fun remoteIpReputationReason_mapsToLocalizedString() {
        val text =
            reasonLabelText(
                code = "remote_ip_reputation_flagged",
                resolve = { resId ->
                    assertEquals(R.string.reason_remote_ip_reputation_flagged, resId)
                    "A validação do destino do link reforça a suspeita."
                },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("A validação do destino do link reforça a suspeita.", text)
    }

    @Test
    fun cleanDestinationReason_usesNonAbsoluteTrustLanguage() {
        val text =
            reasonLabelText(
                code = "safe_domain",
                resolve = { resId ->
                    assertEquals(R.string.reason_safe_domain, resId)
                    "O destino do link não revelou sinais externos de risco."
                },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("O destino do link não revelou sinais externos de risco.", text)
    }
}
