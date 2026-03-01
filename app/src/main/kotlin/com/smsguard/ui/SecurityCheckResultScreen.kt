package com.smsguard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smsguard.R
import com.smsguard.core.RiskLevel
import com.smsguard.ui.theme.SMSGuardTheme
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Immutable
data class SecurityCheckResultUi(
    val riskLevel: RiskLevel,
    val domain: String,
    val senderName: String,
    val score: Int,
    val reasons: List<String>,
)

enum class SecurityStatusTone {
    CALM,
    ATTENTION,
    DANGER,
}

@Immutable
data class SecurityCheckContent(
    val tone: SecurityStatusTone,
    @StringRes val chipLabelResId: Int,
    @StringRes val supportingMessageResId: Int,
    @StringRes val primaryActionLabelResId: Int,
    val showsCopyLinkAction: Boolean,
)

@Immutable
private data class RiskUiSpec(
    val icon: @Composable () -> Unit,
    val chipLabel: String,
    val message: String,
    val primaryCta: String,
    val chipContainer: @Composable (ColorScheme) -> Color,
    val chipContent: @Composable (ColorScheme) -> Color,
)

fun securityCheckContentFor(riskLevel: RiskLevel): SecurityCheckContent =
    when (riskLevel) {
        RiskLevel.LOW ->
            SecurityCheckContent(
                tone = SecurityStatusTone.CALM,
                chipLabelResId = R.string.risk_label_low,
                supportingMessageResId = R.string.risk_low_message,
                primaryActionLabelResId = R.string.security_check_action_ignore_exit,
                showsCopyLinkAction = false,
            )
        RiskLevel.MEDIUM ->
            SecurityCheckContent(
                tone = SecurityStatusTone.ATTENTION,
                chipLabelResId = R.string.risk_label_medium,
                supportingMessageResId = R.string.risk_medium_message,
                primaryActionLabelResId = R.string.security_check_action_ignore_exit,
                showsCopyLinkAction = false,
            )
        RiskLevel.HIGH ->
            SecurityCheckContent(
                tone = SecurityStatusTone.DANGER,
                chipLabelResId = R.string.risk_label_high,
                supportingMessageResId = R.string.risk_high_message,
                primaryActionLabelResId = R.string.security_check_action_ignore_exit,
                showsCopyLinkAction = false,
            )
    }

@Composable
private fun riskUiSpec(level: RiskLevel): RiskUiSpec {
    val content = securityCheckContentFor(level)

    return when (level) {
        RiskLevel.LOW ->
            RiskUiSpec(
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = stringResource(R.string.cd_risk_low),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                },
                chipLabel = stringResource(content.chipLabelResId),
                message = stringResource(content.supportingMessageResId),
                primaryCta = stringResource(content.primaryActionLabelResId),
                chipContainer = { cs -> cs.primaryContainer },
                chipContent = { cs -> cs.onPrimaryContainer },
            )
        RiskLevel.MEDIUM ->
            RiskUiSpec(
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = stringResource(R.string.cd_risk_medium),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                },
                chipLabel = stringResource(content.chipLabelResId),
                message = stringResource(content.supportingMessageResId),
                primaryCta = stringResource(content.primaryActionLabelResId),
                chipContainer = { cs -> cs.tertiaryContainer },
                chipContent = { cs -> cs.onTertiaryContainer },
            )
        RiskLevel.HIGH ->
            RiskUiSpec(
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = stringResource(R.string.cd_risk_high),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                },
                chipLabel = stringResource(content.chipLabelResId),
                message = stringResource(content.supportingMessageResId),
                primaryCta = stringResource(content.primaryActionLabelResId),
                chipContainer = { cs -> cs.errorContainer },
                chipContent = { cs -> cs.onErrorContainer },
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityCheckResultScreen(
    riskLevel: RiskLevel,
    domain: String,
    senderName: String,
    score: Int,
    reasons: List<String>,
    onPrimary: () -> Unit,
    onHelp: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
) {
    val ui = riskUiSpec(riskLevel)
    var reasonsExpanded by rememberSaveable { mutableStateOf(false) }
    val resolvedTitle = title ?: stringResource(R.string.security_check_title)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(resolvedTitle) },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
        bottomBar = {
            BottomActionBar(
                primaryLabel = ui.primaryCta,
                onPrimary = onPrimary,
                onHelp = onHelp,
            )
        },
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                    ui.icon()

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusChip(
                            label = ui.chipLabel,
                            containerColor = ui.chipContainer(MaterialTheme.colorScheme),
                            contentColor = ui.chipContent(MaterialTheme.colorScheme),
                        )

                        Text(
                            text = ui.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

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
                    Text(
                        text = stringResource(R.string.analyzed_link_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    SelectionContainer {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    DetailRow(label = stringResource(R.string.sender_label), value = senderName)
                    DetailRow(label = stringResource(R.string.score_label), value = score.toString())
                }
            }

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
                            text = stringResource(R.string.analysis_reasons_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { reasonsExpanded = !reasonsExpanded }) {
                            Text(
                                stringResource(
                                    if (reasonsExpanded) R.string.button_hide else R.string.button_show
                                )
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
                                    ReasonBullet(text = reasonLabel(reason))
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
private fun BottomActionBar(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onHelp: () -> Unit,
) {
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
            FilledTonalButton(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(primaryLabel)
            }

            TextButton(
                onClick = onHelp,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(stringResource(R.string.security_check_action_ask_help))
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
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
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ReasonBullet(text: String) {
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

@Composable
internal fun reasonLabel(code: String): String =
    reasonLabelResId(code)?.let { stringResource(it) } ?: code

@StringRes
internal fun reasonLabelResId(code: String): Int? =
    when (code) {
        "keyword_urgency" -> R.string.reason_keyword_urgency
        "keyword_threat" -> R.string.reason_keyword_threat
        "keyword_payment" -> R.string.reason_keyword_payment
        "keyword_dataRequest" -> R.string.reason_keyword_data_request
        "keyword_publicServices" -> R.string.reason_keyword_public_services
        "keyword_delivery" -> R.string.reason_keyword_delivery
        "keyword_banking" -> R.string.reason_keyword_banking
        "url_present" -> R.string.reason_url_present
        "url_shortener" -> R.string.reason_shortener
        "url_suspicious_tld" -> R.string.reason_suspicious_tld
        "url_punycode" -> R.string.reason_punycode
        "url_non_latin_hostname" -> R.string.reason_non_latin_hostname
        "url_mixed_latin_cyrillic" -> R.string.reason_mixed_latin_cyrillic
        "mb_payment_request" -> R.string.reason_multibanco_payment
        "mb_has_entity_ref" -> R.string.reason_mb_entity_reference
        "mb_has_amount" -> R.string.reason_mb_amount
        "mb_unknown_entity" -> R.string.reason_mb_unknown_entity
        "mb_intermediary_entity" -> R.string.reason_mb_intermediary_entity
        "mb_known_entity" -> R.string.reason_mb_known_entity
        "correlation_brand_entity_mismatch" -> R.string.reason_brand_entity_mismatch
        "correlation_brand_url_mismatch" -> R.string.reason_brand_url_mismatch
        "data_request_minimum_medium" -> R.string.reason_data_request_minimum_medium
        "non_latin_url_minimum_medium" -> R.string.reason_non_latin_url_minimum_medium
        "safe_domain" -> R.string.reason_safe_domain
        "shortener" -> R.string.reason_shortener
        "suspicious_tld" -> R.string.reason_suspicious_tld
        "punycode" -> R.string.reason_punycode
        "trigger_words" -> R.string.reason_trigger_words
        "brand_impersonation" -> R.string.reason_brand_impersonation
        "weird_structure" -> R.string.reason_weird_structure
        "multibanco_payment" -> R.string.reason_multibanco_payment
        else -> null
    }

internal fun reasonLabelText(
    code: String,
    resolve: (Int) -> String,
): String = reasonLabelResId(code)?.let(resolve) ?: code

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLow() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        SecurityCheckResultScreen(
            riskLevel = RiskLevel.LOW,
            domain = stringResource(R.string.preview_domain_amnistia),
            senderName = stringResource(R.string.preview_sender_amnistia),
            score = 20,
            reasons = listOf("safe_domain", "url_present"),
            onPrimary = {},
            onHelp = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewMedium() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        SecurityCheckResultScreen(
            riskLevel = RiskLevel.MEDIUM,
            domain = stringResource(R.string.preview_domain_ctt),
            senderName = stringResource(R.string.preview_sender_ctt),
            score = 55,
            reasons = listOf("correlation_brand_url_mismatch", "keyword_urgency"),
            onPrimary = {},
            onHelp = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewHigh() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        SecurityCheckResultScreen(
            riskLevel = RiskLevel.HIGH,
            domain = stringResource(R.string.preview_domain_paypal),
            senderName = stringResource(R.string.preview_sender_paypal),
            score = 92,
            reasons = listOf("url_punycode", "keyword_dataRequest"),
            onPrimary = {},
            onHelp = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLowProjectTheme() {
    SMSGuardTheme {
        SecurityCheckResultScreen(
            riskLevel = RiskLevel.LOW,
            domain = stringResource(R.string.preview_domain_amnistia),
            senderName = stringResource(R.string.preview_sender_amnistia),
            score = 20,
            reasons = listOf("safe_domain", "url_present"),
            onPrimary = {},
            onHelp = {},
        )
    }
}
