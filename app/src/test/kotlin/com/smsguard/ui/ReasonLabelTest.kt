package com.smsguard.ui

import com.smsguard.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReasonLabelTest {

    @Test
    fun knownTechnicalReason_mapsToLocalizedString() {
        val text =
            reasonLabelText(
                code = "keyword_dataRequest",
                resolve = { resId ->
                    assertEquals(R.string.reason_keyword_data_request, resId)
                    "Pedido de código/PIN/dados sensíveis."
                },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("Pedido de código/PIN/dados sensíveis.", text)
    }

    @Test
    fun unknownTechnicalReason_usesSafeFallback() {
        val text =
            reasonLabelText(
                code = "bank_withdrawal_callback_scam",
                resolve = { error("Should not resolve unknown code") },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("Mensagem considerada suspeita pelas regras de segurança.", text)
        assertFalse(text.contains("bank_withdrawal_callback_scam"))
    }

    @Test
    fun humanReadableReason_isPreserved() {
        val text =
            reasonLabelText(
                code = "Mensagem com padrão típico de fraude bancária",
                resolve = { error("Should not resolve human-readable text") },
                fallback = "Mensagem considerada suspeita pelas regras de segurança.",
            )

        assertEquals("Mensagem com padrão típico de fraude bancária", text)
    }
}
