package com.smsguard.ui

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smsguard.R
import com.smsguard.core.AlertType
import com.smsguard.core.RiskLevel
import com.smsguard.ui.theme.SMSGuardTheme

@StringRes
fun helpShareTemplateResIdFor(riskLevel: RiskLevel): Int =
    when (riskLevel) {
        RiskLevel.LOW -> R.string.help_share_template_low
        RiskLevel.MEDIUM -> R.string.help_share_template_medium
        RiskLevel.HIGH -> R.string.help_share_template_high
    }

fun summarizeHelpReasons(
    reasons: List<String>,
    maxItems: Int = 3,
): String =
    reasons
        .asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .take(maxItems)
        .joinToString(separator = "; ")

fun buildHelpShareMessage(
    intro: String,
    domainLabel: String,
    domain: String,
    missingDomain: String,
    senderLabel: String,
    sender: String,
    scoreLabel: String,
    score: Int,
    reasonsLabel: String,
    reasonsSummary: String,
    noClickNote: String,
    question: String,
): String =
    buildList {
        add(intro)
        add("$domainLabel: ${domain.ifBlank { missingDomain }}")
        sender.trim().takeIf { it.isNotEmpty() }?.let { add("$senderLabel: $it") }
        add("$scoreLabel: $score")
        reasonsSummary.takeIf { it.isNotBlank() }?.let { add("$reasonsLabel: $it") }
        add(noClickNote)
        add(question)
    }.joinToString(separator = "\n")

class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sender =
            intent.getStringExtra("sender")
                ?: intent.getStringExtra("domain")
                ?: getString(R.string.unknown)

        val url = intent.getStringExtra("url") ?: ""
        val messageText = intent.getStringExtra("message_text").orEmpty()

        val score =
            intent.getIntExtra("score", -1)
                .takeIf { it >= 0 }
                ?: 0

        val riskLevel =
            resolveAlertRiskLevel(
                levelExtra = intent.getStringExtra("level"),
                riskLevelExtra = intent.getStringExtra("risk_level"),
            )

        val reasons =
            intent.getStringArrayListExtra("reasons")?.toList()
                ?: intent.getStringArrayExtra("reasons")?.toList()
                ?: emptyList()

        val alertType =
            runCatching {
                AlertType.valueOf(intent.getStringExtra("alert_type").orEmpty())
            }.getOrDefault(AlertType.URL)

        val mbEntidade = intent.getStringExtra("mb_entidade").orEmpty()
        val mbReferencia = intent.getStringExtra("mb_referencia").orEmpty()
        val mbValor = intent.getStringExtra("mb_valor")

        val domain =
            intent.getStringExtra("domain")
                ?: run {
                    val host = try {
                        java.net.URI(url).host
                    } catch (_: Exception) {
                        null
                    }
                    host?.removePrefix("www.") ?: ""
                }

        setContent {
            SMSGuardTheme {
                if (alertType == AlertType.MULTIBANCO) {
                    MultibancoAlertScreen(
                        sender = sender,
                        score = score,
                        reasons = reasons,
                        mbEntidade = mbEntidade,
                        mbReferencia = mbReferencia,
                        mbValor = mbValor,
                        onClose = { finish() },
                        onHelp = {
                            pedirAjuda(
                                sender = sender,
                                messageText = messageText,
                                link = url,
                                alertType = alertType,
                                mbEntidade = mbEntidade,
                                mbReferencia = mbReferencia,
                                mbValor = mbValor,
                            )
                        },
                    )
                } else {
                    SecurityCheckResultScreen(
                        riskLevel = riskLevel,
                        domain = domain,
                        senderName = sender,
                        score = score,
                        reasons = reasons,
                        onPrimary = { finish() },
                        onHelp = {
                            shareSecurityCheckHelp(
                                riskLevel = riskLevel,
                                domain = domain,
                                sender = sender,
                                score = score,
                                reasons = reasons,
                            )
                        },
                    )
                }
            }
        }
    }

    private fun pedirAjuda(
        sender: String,
        messageText: String,
        link: String,
        alertType: AlertType,
        mbEntidade: String,
        mbReferencia: String,
        mbValor: String?,
    ) {
        val safeLink = link.trim()

        val message =
            buildString {
                appendLine(getString(R.string.help_share_intro))
                appendLine()
                appendLine(getString(R.string.help_share_request))
                appendLine()
                appendLine(getString(R.string.help_share_sender, sender))
                appendLine()
                if (messageText.isNotBlank()) {
                    appendLine(getString(R.string.help_share_full_message))
                    appendLine(messageText)
                    appendLine()
                }
                if (alertType == AlertType.MULTIBANCO) {
                    appendLine(getString(R.string.mb_payment_title))
                    if (mbEntidade.isNotBlank()) {
                        append(getString(R.string.mb_entity))
                        append(": ")
                        appendLine(mbEntidade)
                    }
                    if (mbReferencia.isNotBlank()) {
                        append(getString(R.string.mb_reference))
                        append(": ")
                        appendLine(mbReferencia)
                    }
                    if (!mbValor.isNullOrBlank()) {
                        append(getString(R.string.mb_amount))
                        append(": ")
                        appendLine(mbValor)
                    }
                    appendLine()
                }
                if (safeLink.isNotBlank()) {
                    appendLine(safeLink)
                }
            }.trimEnd()

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }

        startActivity(Intent.createChooser(intent, getString(R.string.ask_help)))
    }

    private fun shareSecurityCheckHelp(
        riskLevel: RiskLevel,
        domain: String,
        sender: String,
        score: Int,
        reasons: List<String>,
    ) {
        val displayReasons =
            summarizeHelpReasons(
                reasons =
                    reasons.map { reason ->
                        reasonLabelText(
                            reason,
                            resolve = { getString(it) },
                            fallback = getString(R.string.reason_generic_suspicious),
                        )
                    },
            )

        val message =
            buildHelpShareMessage(
                intro = getString(helpShareTemplateResIdFor(riskLevel)),
                domainLabel = getString(R.string.help_share_domain_label),
                domain = domain.trim(),
                missingDomain = getString(R.string.help_share_domain_missing),
                senderLabel = getString(R.string.sender_label),
                sender = sender.takeUnless { it == getString(R.string.unknown) }.orEmpty(),
                scoreLabel = getString(R.string.score_label),
                score = score,
                reasonsLabel = getString(R.string.help_share_reasons_label),
                reasonsSummary = displayReasons,
                noClickNote = getString(R.string.help_share_no_click_note),
                question = getString(R.string.help_share_request_confirmation),
            )

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.help_share_subject))
                putExtra(Intent.EXTRA_TEXT, message)
            }

        startActivity(
            Intent.createChooser(intent, getString(R.string.help_share_chooser_title)),
        )
    }
}

internal fun resolveAlertRiskLevel(
    levelExtra: String?,
    riskLevelExtra: String?,
): RiskLevel {
    val rawLevel =
        levelExtra
            ?.takeIf { it.isNotBlank() }
            ?: riskLevelExtra?.takeIf { it.isNotBlank() }
            ?: return RiskLevel.LOW

    return runCatching {
        RiskLevel.valueOf(rawLevel)
    }.getOrDefault(RiskLevel.LOW)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultibancoAlertScreen(
    sender: String,
    score: Int,
    reasons: List<String>,
    mbEntidade: String,
    mbReferencia: String,
    mbValor: String?,
    onClose: () -> Unit,
    onHelp: () -> Unit,
) {
    var reasonsExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.mb_payment_title)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onHelp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.ask_help))
                    }
                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.alert_close))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Risk status card
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = stringResource(R.string.risk_label_high),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                        val warningMessage =
                            stringResource(R.string.mb_warning_line1) + " " +
                                stringResource(R.string.mb_warning_line2) + "\n" +
                                stringResource(R.string.mb_warning_line3)
                        Text(
                            text = warningMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Payment details card
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (mbEntidade.isNotBlank()) {
                        MbDetailRow(
                            label = stringResource(R.string.mb_entity),
                            value = mbEntidade,
                            emphasize = true,
                        )
                    }
                    if (mbReferencia.isNotBlank()) {
                        MbDetailRow(
                            label = stringResource(R.string.mb_reference),
                            value = mbReferencia,
                            emphasize = true,
                        )
                    }
                    if (!mbValor.isNullOrBlank()) {
                        MbDetailRow(
                            label = stringResource(R.string.mb_amount),
                            value = "$mbValor€",
                            emphasize = true,
                        )
                    }
                    MbDetailRow(label = stringResource(R.string.alert_sender), value = sender)
                    MbDetailRow(label = stringResource(R.string.alert_score), value = score.toString())
                }
            }

            // Reasons card (expandable)
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.alert_reasons),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { reasonsExpanded = !reasonsExpanded }) {
                            Text(
                                stringResource(
                                    if (reasonsExpanded) R.string.button_hide else R.string.button_show,
                                ),
                            )
                        }
                    }
                    AnimatedVisibility(visible = reasonsExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (reasons.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.analysis_no_details),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                reasons.forEach { reason ->
                                    MbReasonBullet(text = reasonLabel(reason))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MbDetailRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 80.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else null,
            color = if (emphasize) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MbReasonBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "\u2022",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
