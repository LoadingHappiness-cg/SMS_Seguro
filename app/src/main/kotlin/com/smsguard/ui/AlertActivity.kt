package com.smsguard.ui

import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            runCatching {
                RiskLevel.valueOf(
                    intent.getStringExtra("level")
                        ?: intent.getStringExtra("risk_level")
                        ?: RiskLevel.MEDIUM.name,
                )
            }.getOrDefault(RiskLevel.MEDIUM)

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
                reasons = reasons.map { reason -> reasonLabelText(reason) { getString(it) } },
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE65100))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.mb_payment_title),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (mbEntidade.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.mb_entity)}: $mbEntidade",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C),
                    )
                }
                if (mbReferencia.isNotBlank()) {
                    Text(
                        text = "${stringResource(R.string.mb_reference)}: $mbReferencia",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C),
                    )
                }
                if (!mbValor.isNullOrBlank()) {
                    Text(
                        text = "${stringResource(R.string.mb_amount)}: $mbValor€",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${stringResource(R.string.alert_sender)}: $sender",
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                )
                Text(
                    text = "${stringResource(R.string.alert_score)}: $score",
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.alert_reasons),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black,
                )
                reasons.forEach { reason ->
                    Text(
                        text = "\u2022 ${reasonLabel(reason)}",
                        fontSize = 18.sp,
                        color = Color.DarkGray,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onHelp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Text(stringResource(R.string.ask_help), fontSize = 20.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onClose) {
            Text(
                text = stringResource(R.string.alert_close),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
