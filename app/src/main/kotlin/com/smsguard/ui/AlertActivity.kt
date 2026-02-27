package com.smsguard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
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
import com.smsguard.ui.theme.SMSGuardTheme

class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sender =
            intent.getStringExtra("sender")
                ?: intent.getStringExtra("domain")
                ?: getString(R.string.unknown)

        val url = intent.getStringExtra("url") ?: ""

        val score =
            intent.getIntExtra("score", -1)
                .takeIf { it >= 0 }
                ?: 0

        val riskLevel =
            intent.getStringExtra("level")
                ?: intent.getStringExtra("risk_level")
                ?: ""

        val reasons =
            intent.getStringArrayListExtra("reasons")?.toTypedArray()
                ?: intent.getStringArrayExtra("reasons")
                ?: emptyArray()

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
                AlertScreen(
                    alertType = alertType,
                    riskLevel = riskLevel,
                    domain = domain,
                    sender = sender,
                    score = score,
                    reasons = reasons.toList(),
                    mbEntidade = mbEntidade,
                    mbReferencia = mbReferencia,
                    mbValor = mbValor,
                    hasUrl = url.isNotBlank(),
                    onClose = { finish() },
                    onOpen = {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {
                            Toast.makeText(this, getString(R.string.toast_no_browser), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onHelp = {
                        pedirAjuda(
                            link = url,
                            alertType = alertType,
                            mbEntidade = mbEntidade,
                            mbReferencia = mbReferencia,
                            mbValor = mbValor
                        )
                    }
                )
            }
        }
    }

    private fun pedirAjuda(
        link: String,
        alertType: AlertType,
        mbEntidade: String,
        mbReferencia: String,
        mbValor: String?
    ) {
        val safeLink = link.trim()

        val message =
            buildString {
                appendLine("Recebi um SMS suspeito.")
                appendLine()
                appendLine("Podes verificar se é seguro?")
                appendLine()
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

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }

        startActivity(
            Intent.createChooser(
                intent,
                getString(R.string.ask_help)
            )
        )
    }
}

@Composable
fun AlertScreen(
    alertType: AlertType,
    riskLevel: String,
    domain: String,
    sender: String,
    score: Int,
    reasons: List<String>,
    mbEntidade: String,
    mbReferencia: String,
    mbValor: String?,
    hasUrl: Boolean,
    onClose: () -> Unit,
    onOpen: () -> Unit,
    onHelp: () -> Unit
) {
    val bgColor = if (riskLevel == "HIGH") Color(0xFFB71C1C) else Color(0xFFE65100)
    val riskLabel =
        when (riskLevel) {
            "HIGH" -> stringResource(R.string.risk_high)
            "MEDIUM" -> stringResource(R.string.risk_medium)
            "LOW" -> stringResource(R.string.risk_low)
            else -> stringResource(R.string.risk_unknown)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text =
                if (alertType == AlertType.MULTIBANCO)
                    stringResource(R.string.mb_payment_title)
                else
                    stringResource(R.string.alert_danger_detected),
            color = Color.White,
            fontSize = if (alertType == AlertType.MULTIBANCO) 28.sp else 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        if (alertType != AlertType.MULTIBANCO) {
            Text(
                text = stringResource(R.string.alert_risk_level, riskLabel),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (alertType == AlertType.MULTIBANCO) {
                    Text(
                        text = stringResource(R.string.mb_payment_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (mbEntidade.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.mb_entity)}: $mbEntidade",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                    }
                    if (mbReferencia.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.mb_reference)}: $mbReferencia",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                    }
                    if (!mbValor.isNullOrBlank()) {
                        Text(
                            text = "${stringResource(R.string.mb_amount)}: $mbValor€",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.mb_warning_line1),
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = stringResource(R.string.mb_warning_line2),
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = stringResource(R.string.mb_warning_line3),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                } else {
                    Text(
                        text = stringResource(R.string.alert_detected_link),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = domain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${stringResource(R.string.alert_sender)}: $sender",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "${stringResource(R.string.alert_score)}: $score",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.alert_reasons),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                reasons.forEach { reason ->
                    Text(text = "• ${reasonLabel(reason)}", fontSize = 18.sp, color = Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text(stringResource(R.string.ask_help), fontSize = 20.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hasUrl) {
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
            ) {
                Text(stringResource(R.string.open_link), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onClose) {
            Text(
                stringResource(R.string.alert_close),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun reasonLabel(code: String): String =
    when (code) {
        "keyword_urgency" -> stringResource(R.string.reason_keyword_urgency)
        "keyword_threat" -> stringResource(R.string.reason_keyword_threat)
        "keyword_payment" -> stringResource(R.string.reason_keyword_payment)
        "keyword_dataRequest" -> stringResource(R.string.reason_keyword_data_request)
        "keyword_publicServices" -> stringResource(R.string.reason_keyword_public_services)
        "keyword_delivery" -> stringResource(R.string.reason_keyword_delivery)
        "keyword_banking" -> stringResource(R.string.reason_keyword_banking)
        "url_present" -> stringResource(R.string.reason_url_present)
        "url_shortener" -> stringResource(R.string.reason_shortener)
        "url_suspicious_tld" -> stringResource(R.string.reason_suspicious_tld)
        "url_punycode" -> stringResource(R.string.reason_punycode)
        "url_non_latin_hostname" -> stringResource(R.string.reason_non_latin_hostname)
        "url_mixed_latin_cyrillic" -> stringResource(R.string.reason_mixed_latin_cyrillic)
        "mb_payment_request" -> stringResource(R.string.reason_multibanco_payment)
        "mb_has_entity_ref" -> stringResource(R.string.reason_mb_entity_reference)
        "mb_has_amount" -> stringResource(R.string.reason_mb_amount)
        "mb_unknown_entity" -> stringResource(R.string.reason_mb_unknown_entity)
        "mb_intermediary_entity" -> stringResource(R.string.reason_mb_intermediary_entity)
        "mb_known_entity" -> stringResource(R.string.reason_mb_known_entity)
        "correlation_brand_entity_mismatch" -> stringResource(R.string.reason_brand_entity_mismatch)
        "correlation_brand_url_mismatch" -> stringResource(R.string.reason_brand_url_mismatch)
        "safe_domain" -> stringResource(R.string.reason_safe_domain)
        "shortener" -> stringResource(R.string.reason_shortener)
        "suspicious_tld" -> stringResource(R.string.reason_suspicious_tld)
        "punycode" -> stringResource(R.string.reason_punycode)
        "trigger_words" -> stringResource(R.string.reason_trigger_words)
        "brand_impersonation" -> stringResource(R.string.reason_brand_impersonation)
        "weird_structure" -> stringResource(R.string.reason_weird_structure)
        "multibanco_payment" -> stringResource(R.string.reason_multibanco_payment)
        else -> code
    }
