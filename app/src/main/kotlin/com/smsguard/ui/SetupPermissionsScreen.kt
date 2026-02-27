package com.smsguard.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smsguard.R
import com.smsguard.core.PermissionHealth

@Composable
fun SetupPermissionsScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val needsPostNotifications = remember { mutableStateOf(false) }
    val notificationsEnabled = remember { mutableStateOf(false) }
    val hasListener = remember { mutableStateOf(false) }
    val isIgnoringBatteryOptimizations = remember { mutableStateOf(true) }
    val hasReceiveSmsPermission = remember { mutableStateOf(false) }

    val showNotificationsDeniedMsg = remember { mutableStateOf(false) }
    val showReceiveSmsDeniedMsg = remember { mutableStateOf(false) }

    fun refresh() {
        val health = PermissionHealth(context)
        needsPostNotifications.value = health.needsPostNotifications
        notificationsEnabled.value = health.notificationsEnabled
        hasListener.value = health.hasNotificationListenerAccess
        isIgnoringBatteryOptimizations.value = health.isIgnoringBatteryOptimizations
        hasReceiveSmsPermission.value = health.hasReceiveSmsPermission
    }

    val requestNotificationsPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            showNotificationsDeniedMsg.value = !granted
            refresh()
        }

    val requestReceiveSmsPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            showReceiveSmsDeniedMsg.value = !granted
            refresh()
        }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    val step1Ok = !needsPostNotifications.value && notificationsEnabled.value
    val step2Ok = hasListener.value
    val canContinue = step1Ok && step2Ok
    val isXiaomi = remember { context.isXiaomiDevice() }

    SetupPermissionsScaffold(
        modifier = modifier,
        step1Ok = step1Ok,
        step2Ok = step2Ok,
        step3Ok = isIgnoringBatteryOptimizations.value,
        showXiaomiAutoStart = isXiaomi,
        showNotificationsDeniedMsg = showNotificationsDeniedMsg,
        showReceiveSmsDeniedMsg = showReceiveSmsDeniedMsg,
        onNotificationsClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && needsPostNotifications.value) {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@SetupPermissionsScaffold
            }

            // For < Android 13 or if permission already granted but notifications are disabled, open settings.
            context.openAppNotificationSettings()
        },
        onListenerClick = {
            context.openIntentSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        },
        stepSmsOk = hasReceiveSmsPermission.value,
        onReceiveSmsClick = {
            requestReceiveSmsPermission.launch(Manifest.permission.RECEIVE_SMS)
        },
        onBatteryClick = {
            context.openBatteryHelp()
        },
        onXiaomiAutoStartClick = {
            context.openXiaomiAutoStartSettings()
        },
        onOpenSettingsClick = {
            context.openAppNotificationSettings()
        },
        onContinue = {
            refresh()
            if (canContinue) onContinue()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupPermissionsScaffold(
    step1Ok: Boolean,
    step2Ok: Boolean,
    stepSmsOk: Boolean,
    step3Ok: Boolean,
    showXiaomiAutoStart: Boolean,
    showNotificationsDeniedMsg: MutableState<Boolean>,
    showReceiveSmsDeniedMsg: MutableState<Boolean>,
    onNotificationsClick: () -> Unit,
    onListenerClick: () -> Unit,
    onReceiveSmsClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onXiaomiAutoStartClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showDenied = showNotificationsDeniedMsg.value

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    BrandHeader(
                        iconSize = 32.dp,
                        textSize = 22.sp,
                        stacked = false,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_subtitle),
                fontSize = 18.sp,
            )

            PermissionStepCard(
                title = stringResource(R.string.step_notifications),
                ok = step1Ok,
                buttonText = stringResource(R.string.allow_notifications),
                icon = Icons.Default.Notifications,
                onClick = onNotificationsClick,
            )

            if (showDenied) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.size(10.dp))
                            Text(
                                text = stringResource(R.string.notifications_needed_msg),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenSettingsClick,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(Modifier.size(10.dp))
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                }
            }

            PermissionStepCard(
                title = stringResource(R.string.step_listener),
                ok = step2Ok,
                buttonText = stringResource(R.string.enable_listener),
                icon = Icons.Default.Shield,
                onClick = onListenerClick,
            )

            PermissionStepCard(
                title = stringResource(R.string.step_sms_fallback),
                ok = stepSmsOk,
                buttonText = stringResource(R.string.allow_sms_fallback),
                icon = Icons.Default.Notifications,
                onClick = onReceiveSmsClick,
            )

            if (showReceiveSmsDeniedMsg.value) {
                Text(
                    text = stringResource(R.string.sms_fallback_denied_msg),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            PermissionStepCard(
                title = stringResource(R.string.step_battery),
                ok = step3Ok,
                buttonText = stringResource(R.string.battery_allow),
                icon = Icons.Default.Settings,
                onClick = onBatteryClick,
            )

            if (showXiaomiAutoStart) {
                PermissionStepCard(
                    title = stringResource(R.string.step_autostart_xiaomi),
                    ok = true,
                    buttonText = stringResource(R.string.enable_autostart_xiaomi),
                    icon = Icons.Default.Settings,
                    onClick = onXiaomiAutoStartClick,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onContinue,
                enabled = step1Ok && step2Ok,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) {
                Text(
                    text = stringResource(R.string.continue_btn),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PermissionStepCard(
    title: String,
    ok: Boolean,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = if (ok) "✅" else "❌",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Button(
                onClick = onClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun Context.openIntentSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        openAppDetails()
    }
}

private fun Context.openBatteryHelp() {
    // Best-effort, OEM-safe approach:
    // - First: request ignoring battery optimizations for this app (shows a system dialog)
    // - Fallback: open app details (OEMs usually expose battery/autostart from there)
    // - Xiaomi: try opening MIUI AutoStart management screen (best effort)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoring = pm?.isIgnoringBatteryOptimizations(packageName) == true
        if (!isIgnoring) {
            val intent =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            openIntentSafely(intent)
            return
        }
    }

    if (isXiaomiDevice()) {
        openXiaomiAutoStartSettings()
        return
    }

    openAppDetails()
}

private fun Context.openXiaomiAutoStartSettings() {
    val miuiIntent =
        Intent("miui.intent.action.OP_AUTO_START").apply {
            component =
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                )
        }

    try {
        startActivity(miuiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        openAppDetails()
    }
}

private fun Context.isXiaomiDevice(): Boolean {
    val manufacturer = (Build.MANUFACTURER ?: "").lowercase()
    return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
}

private fun Context.openAppDetails() {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    startActivity(intent)
}

private fun Context.openAppNotificationSettings() {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    openIntentSafely(intent)
}
