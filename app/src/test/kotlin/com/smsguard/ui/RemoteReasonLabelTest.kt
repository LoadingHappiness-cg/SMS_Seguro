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
                    "O domínio foi bloqueado pelo filtro DNS de segurança."
                },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("O domínio foi bloqueado pelo filtro DNS de segurança.", text)
        assertFalse(text.contains("remote_dns_blocked"))
    }

    @Test
    fun remoteIpReputationReason_mapsToLocalizedString() {
        val text =
            reasonLabelText(
                code = "remote_ip_reputation_flagged",
                resolve = { resId ->
                    assertEquals(R.string.reason_remote_ip_reputation_flagged, resId)
                    "O endereço de destino tem má reputação externa."
                },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("O endereço de destino tem má reputação externa.", text)
    }
}
