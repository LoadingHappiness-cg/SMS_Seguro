package com.smsguard.ui

import com.smsguard.R
import com.smsguard.core.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityCheckResultModelTest {

    @Test
    fun missingRiskLevelExtras_defaultsToLowInsteadOfMedium() {
        assertEquals(
            RiskLevel.LOW,
            resolveAlertRiskLevel(
                levelExtra = null,
                riskLevelExtra = null,
            ),
        )
    }

    @Test
    fun validLowRiskLevelExtra_remainsLow() {
        assertEquals(
            RiskLevel.LOW,
            resolveAlertRiskLevel(
                levelExtra = RiskLevel.LOW.name,
                riskLevelExtra = null,
            ),
        )
    }

    @Test
    fun lowRisk_usesCalmSemanticsAndOkAction() {
        val state = securityCheckContentFor(RiskLevel.LOW)

        assertEquals(SecurityStatusTone.CALM, state.tone)
        assertEquals(R.string.security_check_action_ignore_exit, state.primaryActionLabelResId)
        assertEquals(R.string.risk_low_message, state.supportingMessageResId)
    }

    @Test
    fun mediumRisk_usesAttentionSemanticsAndOkAction() {
        val state = securityCheckContentFor(RiskLevel.MEDIUM)

        assertEquals(SecurityStatusTone.ATTENTION, state.tone)
        assertEquals(R.string.security_check_action_ignore_exit, state.primaryActionLabelResId)
        assertEquals(R.string.risk_medium_message, state.supportingMessageResId)
    }

    @Test
    fun highRisk_usesDangerSemanticsAndOkAction() {
        val state = securityCheckContentFor(RiskLevel.HIGH)

        assertEquals(SecurityStatusTone.DANGER, state.tone)
        assertEquals(R.string.security_check_action_ignore_exit, state.primaryActionLabelResId)
        assertEquals(R.string.risk_high_message, state.supportingMessageResId)
    }

    @Test
    fun mediumRisk_includesChipLabelWithoutCopyAction() {
        val state = securityCheckContentFor(RiskLevel.MEDIUM)

        assertEquals(R.string.risk_label_medium, state.chipLabelResId)
        assertEquals(false, state.showsCopyLinkAction)
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
