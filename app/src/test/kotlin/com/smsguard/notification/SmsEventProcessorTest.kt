package com.smsguard.notification

import com.smsguard.core.AlertType
import com.smsguard.core.RiskEngine
import com.smsguard.core.RiskLevel
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
}
