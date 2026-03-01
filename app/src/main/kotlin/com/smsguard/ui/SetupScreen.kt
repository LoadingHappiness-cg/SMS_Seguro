package com.smsguard.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.smsguard.R
import com.smsguard.core.PermissionHealth
import com.smsguard.core.ProtectionStatusReport
import com.smsguard.core.XiaomiSupportInfo
import com.smsguard.core.xiaomiSupportInfo
import com.smsguard.notification.AlertNotifierChannels
import com.smsguard.notification.ForegroundServiceNotifier
import com.smsguard.rules.RuleLoader
import com.smsguard.update.RuleUpdateScheduler
import com.smsguard.update.RuleUpdateWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SetupScreen(repairRequestNonce: Int = 0) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ruleLoader = remember { RuleLoader(context) }
    val prefs = remember { context.getSharedPreferences("ruleset_meta", Context.MODE_PRIVATE) }
    val workManager = remember { WorkManager.getInstance(context) }
    val xiaomiInfo = remember { context.xiaomiSupportInfo() }

    var rulesetVersion by remember { mutableIntStateOf(ruleLoader.loadCurrent().version) }
    var lastCheckAt by remember { mutableLongStateOf(prefs.getLong("last_check_at", 0L)) }
    var isChecking by remember { mutableStateOf(false) }
    var statusMessageRes by remember { mutableStateOf<Int?>(null) }
    var protectionStatus by remember { mutableStateOf(PermissionHealth(context).protectionStatusReport()) }
    var ignoresBatteryOptimizations by remember { mutableStateOf(PermissionHealth(context).isIgnoringBatteryOptimizations) }
    var showActivationPrompt by remember { mutableStateOf(false) }

    fun refreshProtectionStatus() {
        val health = PermissionHealth(context)
        protectionStatus = health.protectionStatusReport()
        ignoresBatteryOptimizations = health.isIgnoringBatteryOptimizations
    }

    fun runPrimaryRepairAction(report: ProtectionStatusReport = protectionStatus) {
        when (primaryRepairActionFor(report)) {
            ProtectionRepairAction.ENABLE_LISTENER -> showActivationPrompt = true
            ProtectionRepairAction.ENABLE_ALERTS -> context.openAlertDeliverySettings(report)
            ProtectionRepairAction.FIX_FOREGROUND_NOTIFICATION -> context.fixForegroundNotification(report)
            ProtectionRepairAction.NONE -> Unit
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshProtectionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        AlertNotifierChannels.ensure(context)
        ForegroundServiceNotifier.ensureChannel(context)
        RuleUpdateScheduler.schedulePeriodic(context)
        refreshRulesetMeta(prefs, ruleLoader) { version, checkAt ->
            rulesetVersion = version
            lastCheckAt = checkAt
        }
        refreshProtectionStatus()
    }

    LaunchedEffect(repairRequestNonce) {
        if (repairRequestNonce > 0) {
            refreshProtectionStatus()
            runPrimaryRepairAction()
        }
    }

    val checkNowWorkInfoFlow: Flow<WorkInfo?> =
        remember(workManager) {
            workManager
                .getWorkInfosForUniqueWorkFlow(RuleUpdateScheduler.WORK_CHECK_NOW_NAME)
                .map { infos ->
                    infos.firstOrNull { !it.state.isFinished } ?: infos.firstOrNull()
                }
                .distinctUntilChanged()
        }

    val checkNowWorkInfo by checkNowWorkInfoFlow.collectAsState(initial = null)

    LaunchedEffect(checkNowWorkInfo?.state, checkNowWorkInfo?.outputData) {
        val info = checkNowWorkInfo ?: return@LaunchedEffect

        when (info.state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.RUNNING,
            WorkInfo.State.BLOCKED -> {
                isChecking = true
                statusMessageRes = R.string.checking_updates
            }

            WorkInfo.State.SUCCEEDED,
            WorkInfo.State.FAILED,
            WorkInfo.State.CANCELLED -> {
                isChecking = false
                val outcome =
                    info.outputData.getString(RuleUpdateWorker.OUTCOME_KEY)
                        ?: RuleUpdateWorker.OUTCOME_FAILED

                statusMessageRes =
                    when (outcome) {
                        RuleUpdateWorker.OUTCOME_UPDATED -> R.string.rules_updated
                        RuleUpdateWorker.OUTCOME_NOOP -> R.string.rules_up_to_date
                        RuleUpdateWorker.OUTCOME_NO_NETWORK -> R.string.update_no_internet
                        RuleUpdateWorker.OUTCOME_INVALID_SIG -> R.string.update_rejected_security
                        else -> R.string.update_failed
                    }

                refreshRulesetMeta(prefs, ruleLoader) { version, checkAt ->
                    rulesetVersion = version
                    lastCheckAt = checkAt
                }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BrandHeader(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 16.dp, end = 16.dp),
                iconSize = 46.dp,
                titleStyle = MaterialTheme.typography.titleMedium,
                textSize = MaterialTheme.typography.titleMedium.fontSize,
                gap = 12.dp,
                stacked = false,
                subtitle = stringResource(R.string.brand_header_subtitle),
                titleColor = MaterialTheme.colorScheme.onSurface,
                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )

            ProtectionOverviewCard(
                report = protectionStatus,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            ProtectionStatusCard(
                report = protectionStatus,
                onEnableListener = { showActivationPrompt = true },
                onEnableAlerts = { context.openAlertDeliverySettings(protectionStatus) },
                onFixForegroundNotification = { context.fixForegroundNotification(protectionStatus) },
                onCheckRulesNow = {
                    val info = checkNowWorkInfo
                    val isAlreadyRunning =
                        info?.state == WorkInfo.State.ENQUEUED ||
                            info?.state == WorkInfo.State.RUNNING ||
                            info?.state == WorkInfo.State.BLOCKED

                    if (isAlreadyRunning) {
                        statusMessageRes = R.string.already_checking
                        isChecking = true
                        return@ProtectionStatusCard
                    }

                    if (!isNetworkConnected(context)) {
                        statusMessageRes = R.string.update_no_internet
                        isChecking = false
                        return@ProtectionStatusCard
                    }

                    statusMessageRes = R.string.checking_updates
                    isChecking = true
                    RuleUpdateScheduler.runCheckNow(context)
                },
                rulesStatus = rulesUpdateStatus(isChecking, statusMessageRes, context),
                rulesHealthy = rulesUpdateHealthy(isChecking, statusMessageRes),
                lastCheckAt = lastCheckAt,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            if (xiaomiInfo.shouldShowGuidance) {
                XiaomiGuidanceCard(
                    xiaomiInfo = xiaomiInfo,
                    ignoresBatteryOptimizations = ignoresBatteryOptimizations,
                    onOpenBatterySettings = { context.openBatteryOptimizationSettings() },
                    onOpenSystemSettings = { context.openXiaomiAutoStartSettings() },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.setup_privacy_promise),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showActivationPrompt) {
            ProtectionActivationFullScreen(
                showXiaomiNote = xiaomiInfo.shouldShowGuidance,
                onContinue = {
                    showActivationPrompt = false
                    context.openNotificationListenerSettingsWithPrompt()
                },
                onDismiss = { showActivationPrompt = false },
            )
        }
    }
}

@Composable
private fun ProtectionOverviewCard(
    report: ProtectionStatusReport,
    modifier: Modifier = Modifier,
) {
    val ready = report.isReady
    val cardColor =
        if (ready) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    val iconTint =
        if (ready) {
            Color(0xFF2E7D32)
        } else {
            MaterialTheme.colorScheme.tertiary
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (ready) Color(0x332E7D32) else MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Icon(
                    imageVector = if (ready) Icons.Outlined.Verified else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text =
                        stringResource(
                            if (ready) R.string.setup_protection_active else R.string.setup_configuration_incomplete_title,
                        ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        stringResource(
                            if (ready) {
                                R.string.setup_protection_active_supporting
                            } else {
                                R.string.setup_configuration_incomplete_body
                            },
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProtectionStatusCard(
    report: ProtectionStatusReport,
    onEnableListener: () -> Unit,
    onEnableAlerts: () -> Unit,
    onFixForegroundNotification: () -> Unit,
    onCheckRulesNow: () -> Unit,
    rulesStatus: String,
    rulesHealthy: Boolean,
    lastCheckAt: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_system_status_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ProtectionStatusRow(
                label = stringResource(R.string.setup_status_listener_simple),
                ok = report.listenerOk,
            )
            ProtectionStatusRow(
                label = stringResource(R.string.setup_status_alerts_simple),
                ok = report.alertsReady,
            )
            ProtectionStatusRow(
                label = stringResource(R.string.setup_status_foreground_notification_simple),
                ok = report.foregroundNotificationAllowed,
                okText = stringResource(R.string.setup_status_active),
                missingText = stringResource(R.string.setup_status_inactive),
            )
            ProtectionStatusRow(
                label = stringResource(R.string.setup_status_rules_simple),
                ok = rulesHealthy,
                okText = rulesStatus,
                missingText = rulesStatus,
            )

            SetupInfoRow(
                label = stringResource(R.string.last_check),
                value = formatLastCheck(lastCheckAt, context),
            )

            if (!report.listenerOk) {
                FilledTonalButton(
                    onClick = onEnableListener,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.setup_action_activate))
                }
            }

            if (!report.alertsReady) {
                FilledTonalButton(
                    onClick = onEnableAlerts,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.setup_action_activate_alerts))
                }
            }

            if (!report.foregroundNotificationAllowed) {
                FilledTonalButton(
                    onClick = onFixForegroundNotification,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.setup_action_activate_foreground))
                }
            }

            FilledTonalButton(
                onClick = onCheckRulesNow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.setup_action_check_rules))
            }
        }
    }
}

@Composable
private fun ProtectionStatusRow(
    label: String,
    ok: Boolean,
    okText: String = stringResource(R.string.setup_status_ok),
    missingText: String = stringResource(R.string.setup_status_missing),
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 180.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (ok) Icons.Outlined.Verified else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (ok) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            )
            Text(
                text = if (ok) okText else missingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun XiaomiGuidanceCard(
    xiaomiInfo: XiaomiSupportInfo,
    ignoresBatteryOptimizations: Boolean,
    onOpenBatterySettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.xiaomi_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    stringResource(
                        if (xiaomiInfo.isMiuiLike) R.string.xiaomi_section_body_miui else R.string.xiaomi_section_body,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            XiaomiBullet(
                text =
                    if (ignoresBatteryOptimizations) {
                        stringResource(R.string.xiaomi_quick_step_battery_done)
                    } else {
                        stringResource(R.string.xiaomi_quick_step_battery)
                    },
            )
            XiaomiBullet(text = stringResource(R.string.xiaomi_quick_step_autostart))
            XiaomiBullet(text = stringResource(R.string.xiaomi_quick_step_permissions))

            FilledTonalButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.BatterySaver, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.xiaomi_battery_action))
            }

            FilledTonalButton(
                onClick = onOpenSystemSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.xiaomi_system_action))
            }
        }
    }
}

@Composable
private fun XiaomiBullet(
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "\u2022",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SetupInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 160.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

private fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun formatLastCheck(timestampMs: Long, context: Context): String {
    if (timestampMs <= 0L) return context.getString(R.string.never)
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT"))
    return sdf.format(Date(timestampMs))
}

internal fun debugDiagnosticsEnabled(@Suppress("UNUSED_PARAMETER") isDebugBuild: Boolean = false): Boolean = false

private fun rulesUpdateStatus(
    isChecking: Boolean,
    statusMessageRes: Int?,
    context: Context,
): String =
    when {
        isChecking -> context.getString(R.string.setup_rules_status_running)
        statusMessageRes == R.string.rules_updated || statusMessageRes == R.string.rules_up_to_date -> {
            context.getString(R.string.setup_rules_status_ok)
        }
        statusMessageRes == R.string.update_failed || statusMessageRes == R.string.update_rejected_security -> {
            context.getString(R.string.setup_rules_status_error)
        }
        else -> context.getString(R.string.setup_rules_status_unknown)
    }

private fun rulesUpdateHealthy(
    isChecking: Boolean,
    statusMessageRes: Int?,
): Boolean =
    !isChecking &&
        (
            statusMessageRes == R.string.rules_updated ||
                statusMessageRes == R.string.rules_up_to_date
        )

private fun refreshRulesetMeta(
    prefs: android.content.SharedPreferences,
    ruleLoader: RuleLoader,
    onResult: (version: Int, lastCheckAt: Long) -> Unit,
) {
    val version = prefs.getInt("ruleset_version", -1).let { stored ->
        if (stored > 0) stored else ruleLoader.loadCurrent().version
    }
    val lastCheckAt = prefs.getLong("last_check_at", 0L)
    onResult(version, lastCheckAt)
}
