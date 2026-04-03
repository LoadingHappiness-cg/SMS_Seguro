package com.smsguard.storage

import com.smsguard.core.HistoryEvent
import com.smsguard.core.RiskLevel
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
            )

        store.seedHistoryForTest(listOf(event))
        assertTrue(store.getAllEvents().isNotEmpty())

        store.clearProcessedHistory()

        assertTrue(store.getAllEvents().isEmpty())
    }
}
