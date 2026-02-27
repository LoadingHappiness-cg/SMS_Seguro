package com.smsguard.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smsguard.rules.RuleLoader
import com.smsguard.update.RuleUpdateScheduler
import com.smsguard.update.RuleUpdateWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ruleLoader = remember { RuleLoader(context) }
    val prefs = remember { context.getSharedPreferences("ruleset_meta", Context.MODE_PRIVATE) }
    val workManager = remember { WorkManager.getInstance(context) }

    var rulesetVersion by remember { mutableStateOf(ruleLoader.loadCurrent().version) }
    var lastCheckAt by remember { mutableStateOf(prefs.getLong("last_check_at", 0L)) }
    var isChecking by remember { mutableStateOf(false) }
    var statusMessageRes by remember { mutableStateOf<Int?>(null) }
    
    var isNotificationEnabled by remember { 
        mutableStateOf(isNotificationServiceEnabled(context)) 
    }

    // Refresh status when returning to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationEnabled = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        RuleUpdateScheduler.schedulePeriodic(context)
        refreshRulesetMeta(prefs, ruleLoader) { version, checkAt ->
            rulesetVersion = version
            lastCheckAt = checkAt
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
                if (statusMessageRes == null) {
                    statusMessageRes = com.smsguard.R.string.checking_updates
                }
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
                        RuleUpdateWorker.OUTCOME_UPDATED -> com.smsguard.R.string.rules_updated
                        RuleUpdateWorker.OUTCOME_NOOP -> com.smsguard.R.string.rules_up_to_date
                        RuleUpdateWorker.OUTCOME_NO_NETWORK -> com.smsguard.R.string.update_no_internet
                        RuleUpdateWorker.OUTCOME_INVALID_SIG -> com.smsguard.R.string.update_rejected_security
                        else -> com.smsguard.R.string.update_failed
                    }

                refreshRulesetMeta(prefs, ruleLoader) { version, checkAt ->
                    rulesetVersion = version
                    lastCheckAt = checkAt
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(com.smsguard.R.string.setup_app_brand),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        if (!isNotificationEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(com.smsguard.R.string.setup_protection_off_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(com.smsguard.R.string.setup_protection_off_body),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFFC62828)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = stringResource(com.smsguard.R.string.setup_enable_protection_now),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(com.smsguard.R.string.setup_protection_active),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(com.smsguard.R.string.setup_ruleset_info_title), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    text = "${stringResource(com.smsguard.R.string.rules_version)} $rulesetVersion",
                    fontSize = 18.sp
                )
                Text(
                    text = "${stringResource(com.smsguard.R.string.last_check)} ${formatLastCheck(lastCheckAt, context)}",
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        val info = checkNowWorkInfo
                        val isAlreadyRunning =
                            info?.state == WorkInfo.State.ENQUEUED ||
                                info?.state == WorkInfo.State.RUNNING ||
                                info?.state == WorkInfo.State.BLOCKED

                        if (isAlreadyRunning) {
                            statusMessageRes = com.smsguard.R.string.already_checking
                            isChecking = true
                            return@Button
                        }

                        val now = System.currentTimeMillis()
                        prefs.edit().putLong("last_check_at", now).apply()
                        lastCheckAt = now

                        if (!isNetworkConnected(context)) {
                            statusMessageRes = com.smsguard.R.string.update_no_internet
                            isChecking = false
                            return@Button
                        }

                        statusMessageRes = com.smsguard.R.string.checking_updates
                        isChecking = true
                        RuleUpdateScheduler.runCheckNow(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(com.smsguard.R.string.setup_check_updates_now), fontSize = 18.sp)
                }

                if (isChecking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                statusMessageRes?.let { resId ->
                    Text(
                        text = stringResource(resId),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(com.smsguard.R.string.setup_privacy_promise),
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name)
            if (cn != null && pkgName == cn.packageName) {
                return true
            }
        }
    }
    return false
}

private fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun formatLastCheck(timestampMs: Long, context: Context): String {
    if (timestampMs <= 0L) return context.getString(com.smsguard.R.string.never)
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT"))
    return sdf.format(Date(timestampMs))
}

private fun refreshRulesetMeta(
    prefs: android.content.SharedPreferences,
    ruleLoader: RuleLoader,
    onResult: (version: Int, lastCheckAt: Long) -> Unit
) {
    val version = prefs.getInt("ruleset_version", -1).let { stored ->
        if (stored > 0) stored else ruleLoader.loadCurrent().version
    }
    val lastCheckAt = prefs.getLong("last_check_at", 0L)
    onResult(version, lastCheckAt)
}
