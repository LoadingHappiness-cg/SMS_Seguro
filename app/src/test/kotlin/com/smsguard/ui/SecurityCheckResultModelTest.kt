package com.smsguard.ui

import com.smsguard.R
import com.smsguard.core.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityCheckResultModelTest {

    @Test
    fun lowRisk_usesCalmSemanticsAndBrowserAction() {
        val state = securityCheckContentFor(RiskLevel.LOW)

        assertEquals(SecurityStatusTone.CALM, state.tone)
        assertEquals(R.string.security_check_action_open_browser, state.primaryActionLabelResId)
        assertEquals(R.string.security_check_caption_will_open_browser, state.primaryCaptionResId)
        assertEquals(R.string.risk_low_message, state.supportingMessageResId)
    }

    @Test
    fun mediumRisk_usesAttentionSemanticsAndCautiousAction() {
        val state = securityCheckContentFor(RiskLevel.MEDIUM)

        assertEquals(SecurityStatusTone.ATTENTION, state.tone)
        assertEquals(R.string.security_check_action_open_carefully, state.primaryActionLabelResId)
        assertEquals(R.string.security_check_caption_will_open_browser, state.primaryCaptionResId)
        assertEquals(R.string.risk_medium_message, state.supportingMessageResId)
    }

    @Test
    fun highRisk_usesDangerSemanticsAndBlockAction() {
        val state = securityCheckContentFor(RiskLevel.HIGH)

        assertEquals(SecurityStatusTone.DANGER, state.tone)
        assertEquals(R.string.security_check_action_block_link, state.primaryActionLabelResId)
        assertEquals(R.string.security_check_caption_will_not_open, state.primaryCaptionResId)
        assertEquals(R.string.risk_high_message, state.supportingMessageResId)
    }

    @Test
    fun mediumRisk_includesChipLabelWithoutCopyAction() {
        val state = securityCheckContentFor(RiskLevel.MEDIUM)

        assertEquals(R.string.risk_label_medium, state.chipLabelResId)
        assertEquals(false, state.showsCopyLinkAction)
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

    @Test
    fun helpShareTemplate_matchesRiskSemantics() {
        assertEquals(R.string.help_share_template_low, helpShareTemplateResIdFor(RiskLevel.LOW))
        assertEquals(R.string.help_share_template_medium, helpShareTemplateResIdFor(RiskLevel.MEDIUM))
        assertEquals(R.string.help_share_template_high, helpShareTemplateResIdFor(RiskLevel.HIGH))
    }

    @Test
    fun summarizeHelpReasons_limitsItemsAndSeparatesWithSemicolons() {
        val summary = summarizeHelpReasons(listOf("Primeiro", "Segundo", "Terceiro", "Quarto"))

        assertEquals("Primeiro; Segundo; Terceiro", summary)
        assertFalse(summary.contains("Quarto"))
    }

    @Test
    fun buildHelpShareMessage_usesFallbackDomainAndOmitsBlankSender() {
        val message =
            buildHelpShareMessage(
                intro = "Preciso de uma confirmação 🙏",
                domainLabel = "Domínio",
                domain = "",
                missingDomain = "(não detetado)",
                senderLabel = "Remetente",
                sender = " ",
                scoreLabel = "Pontuação",
                score = 20,
                reasonsLabel = "Motivos",
                reasonsSummary = "Urgência incomum; Link presente",
                noClickNote = "Não cliquei no link.",
                question = "Consegues confirmar se é legítimo?",
            )

        assertTrue(message.contains("Domínio: (não detetado)"))
        assertTrue(message.contains("Pontuação: 20"))
        assertTrue(message.contains("Motivos: Urgência incomum; Link presente"))
        assertTrue(message.contains("Não cliquei no link."))
        assertFalse(message.contains("Remetente:"))
    }
}
