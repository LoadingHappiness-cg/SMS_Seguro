package com.smsguard.ui

import com.smsguard.R
import com.smsguard.core.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityCheckResultModelTest {

    @Test
    fun lowRisk_usesCalmSemanticsAndContinueAction() {
        val state = securityCheckContentFor(RiskLevel.LOW)

        assertEquals(SecurityStatusTone.CALM, state.tone)
        assertEquals(R.string.button_continue, state.primaryActionLabelResId)
        assertEquals(R.string.risk_low_message, state.supportingMessageResId)
    }

    @Test
    fun mediumRisk_usesAttentionSemanticsAndCautiousAction() {
        val state = securityCheckContentFor(RiskLevel.MEDIUM)

        assertEquals(SecurityStatusTone.ATTENTION, state.tone)
        assertEquals(R.string.button_open_carefully, state.primaryActionLabelResId)
        assertEquals(R.string.risk_medium_message, state.supportingMessageResId)
    }

    @Test
    fun highRisk_usesDangerSemanticsAndBlockAction() {
        val state = securityCheckContentFor(RiskLevel.HIGH)

        assertEquals(SecurityStatusTone.DANGER, state.tone)
        assertEquals(R.string.button_block, state.primaryActionLabelResId)
        assertEquals(R.string.risk_high_message, state.supportingMessageResId)
    }

    @Test
    fun mediumRisk_includesSecurityHeadlineAndChipLabel() {
        val state = securityCheckContentFor(RiskLevel.MEDIUM)

        assertEquals(R.string.security_check_title, state.headlineResId)
        assertEquals(R.string.risk_label_medium, state.chipLabelResId)
        assertTrue(state.showsCopyLinkAction)
    }

    @Test
    fun lowRisk_primaryAction_opensWithoutWarning() {
        val action = primaryLinkActionFor(RiskLevel.LOW)

        assertTrue(action.shouldOpenUrl)
        assertEquals(null, action.toastMessageResId)
    }

    @Test
    fun mediumRisk_primaryAction_opensWithWarningToast() {
        val action = primaryLinkActionFor(RiskLevel.MEDIUM)

        assertTrue(action.shouldOpenUrl)
        assertEquals(R.string.toast_opening_suspicious_link, action.toastMessageResId)
    }

    @Test
    fun highRisk_primaryAction_blocksWithoutOpening() {
        val action = primaryLinkActionFor(RiskLevel.HIGH)

        assertEquals(false, action.shouldOpenUrl)
        assertEquals(R.string.toast_link_blocked, action.toastMessageResId)
    }
}
