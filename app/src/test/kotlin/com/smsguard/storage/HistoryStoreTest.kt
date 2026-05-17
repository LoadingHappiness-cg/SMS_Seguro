package com.smsguard.storage

import com.smsguard.core.HistoryEvent
import com.smsguard.core.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryStoreTest {

    @Test
    fun clearProcessedHistory_removesStoredEvents() {
        val historyDir = createTempDir(prefix = "history-store-test")
        val store = HistoryStore.forTest(historyDir)

        val event =
            HistoryEvent(
                timestamp = 1L,
                sender = "CGD",
                messageText = "Tentativa de saque: código 038493. Não foi você? Ligue +351300305255",
                score = 80,
                riskLevel = RiskLevel.HIGH,
                linkCount = 2,
            )

        store.seedHistoryForTest(listOf(event))
        assertTrue(store.getAllEvents().isNotEmpty())

        store.clearProcessedHistory()

        assertTrue(store.getAllEvents().isEmpty())
    }

    @Test
    fun seedHistoryForTest_preservesLinkCountForHistoryLabels() {
        val historyDir = createTempDir(prefix = "history-store-test")
        val store = HistoryStore.forTest(historyDir)

        val event =
            HistoryEvent(
                timestamp = 1L,
                sender = "DebugBlocked",
                messageText = "Veja https://example.com e https://apoio-seguro.xyz/login",
                domain = "example.com",
                url = "https://example.com",
                score = 80,
                riskLevel = RiskLevel.HIGH,
                reasons = listOf("url_present", "url_suspicious_tld"),
                linkCount = 2,
            )

        store.seedHistoryForTest(listOf(event))

        val stored = store.getAllEvents().single()
        assertEquals(2, stored.linkCount)
    }
}
