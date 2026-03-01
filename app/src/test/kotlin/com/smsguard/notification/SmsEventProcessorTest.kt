package com.smsguard.notification

import com.smsguard.core.AlertType
import com.smsguard.core.HistoryEvent
import com.smsguard.core.RiskEngine
import com.smsguard.core.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsEventProcessorTest {

    @Test
    fun lowScoreButSuspiciousUrl_stillTriggersNotification() {
        val result =
            RiskEngine.RiskResult(
                score = 35,
                level = RiskLevel.LOW,
                reasons = listOf("url_suspicious_tld", "keyword_delivery"),
                primaryUrl = "https://apoio-entrega.xyz",
                primaryDomain = "apoio-entrega.xyz",
                primaryBrand = "ctt",
            )

        assertTrue(SmsEventProcessor.shouldNotify(AlertType.URL, result))
    }

    @Test
    fun multibancoAlert_stillTriggersNotificationEvenIfLow() {
        val result =
            RiskEngine.RiskResult(
                score = 20,
                level = RiskLevel.LOW,
                reasons = listOf("mb_payment_request"),
                primaryUrl = "",
                primaryDomain = "",
                primaryBrand = null,
            )

        assertTrue(SmsEventProcessor.shouldNotify(AlertType.MULTIBANCO, result))
    }

    @Test
    fun persistAndMaybeNotify_persistsBeforeNotification() {
        val steps = mutableListOf<String>()

        val notified =
            SmsEventProcessor.persistAndMaybeNotify(
                event = sampleHistoryEvent(),
                shouldNotify = true,
                persistEvent = {
                    steps += "persist"
                },
                notifyAlert = {
                    steps += "notify"
                    true
                },
            )

        assertTrue(notified)
        assertEquals(listOf("persist", "notify"), steps)
    }

    @Test
    fun persistAndMaybeNotify_keepsPersistedEventWhenNotificationFails() {
        val steps = mutableListOf<String>()

        val notified =
            SmsEventProcessor.persistAndMaybeNotify(
                event = sampleHistoryEvent(),
                shouldNotify = true,
                persistEvent = {
                    steps += "persist"
                },
                notifyAlert = {
                    steps += "notify"
                    throw IllegalStateException("notify failed")
                },
            )

        assertFalse(notified)
        assertEquals(listOf("persist", "notify"), steps)
    }

    private fun sampleHistoryEvent(): HistoryEvent =
        HistoryEvent(
            timestamp = 1L,
            sender = "CTT",
            messageText = "Veja em https://apoio-entrega.xyz",
            domain = "apoio-entrega.xyz",
            url = "https://apoio-entrega.xyz",
            alertType = AlertType.URL,
            score = 65,
            riskLevel = RiskLevel.MEDIUM,
            reasons = listOf("url_suspicious_tld"),
        )
}
