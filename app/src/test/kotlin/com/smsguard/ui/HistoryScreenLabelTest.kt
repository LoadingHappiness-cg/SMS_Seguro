package com.smsguard.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryScreenLabelTest {

    @Test
    fun historyDomainLabel_showsExtraLinkCountWhenMultipleLinksExist() {
        val label =
            formatHistoryDomainLabel(
                domain = "example.com",
                unknownLabel = "Desconhecido",
                extraLinksLabel = "(+1 link)",
            )

        assertEquals("example.com (+1 link)", label)
    }

    @Test
    fun historyDomainLabel_showsDomainOnlyForSingleLink() {
        val label =
            formatHistoryDomainLabel(
                domain = "example.com",
                unknownLabel = "Desconhecido",
                extraLinksLabel = null,
            )

        assertEquals("example.com", label)
    }
}
