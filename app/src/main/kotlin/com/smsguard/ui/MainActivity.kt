package com.smsguard.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smsguard.core.PermissionHealth
import com.smsguard.startup.SmsProtectionService
import com.smsguard.ui.theme.SMSGuardTheme

class MainActivity : ComponentActivity() {
    private var launchIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchIntentState = intent
        setContent {
            SMSGuardTheme {
                MainScreen(launchIntent = launchIntentState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchIntentState = intent
    }

    companion object {
        const val ACTION_OPEN_PROTECTION = "com.smsguard.OPEN_PROTECTION"
        const val EXTRA_TAB = "tab"
        const val TAB_PROTECTION = "protection"
        const val TAB_ABOUT = "about"
    }
}

@Composable
fun MainScreen(launchIntent: Intent? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by remember { mutableIntStateOf(resolveSelectedTab(launchIntent)) }
    var isProtectionReady by remember { mutableStateOf(PermissionHealth(context).isProtectionReady()) }
    var repairRequestNonce by remember { mutableIntStateOf(0) }

    fun refreshProtectionReady() {
        isProtectionReady = PermissionHealth(context).isProtectionReady()
    }

    LaunchedEffect(launchIntent) {
        selectedTab = resolveSelectedTab(launchIntent)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshProtectionReady()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val serviceIntent = Intent(context, SmsProtectionService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        refreshProtectionReady()
        if (!isProtectionReady) {
            selectedTab = 0
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (!isProtectionReady) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTab = 0
                                repairRequestNonce += 1
                            },
                ) {
                    Text(
                        text = stringResource(com.smsguard.R.string.protection_incomplete),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                val navigationItemColors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(com.smsguard.R.string.cd_setup)) },
                    label = { Text(stringResource(com.smsguard.R.string.tab_setup)) },
                    colors = navigationItemColors,
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    enabled = isProtectionReady,
                    icon = { Icon(Icons.Default.History, contentDescription = stringResource(com.smsguard.R.string.cd_history)) },
                    label = { Text(stringResource(com.smsguard.R.string.tab_history)) },
                    colors = navigationItemColors,
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Info, contentDescription = stringResource(com.smsguard.R.string.cd_about)) },
                    label = { Text(stringResource(com.smsguard.R.string.about_tab_title)) },
                    colors = navigationItemColors,
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.surface,
        ) {
            when (selectedTab) {
                0 -> SetupScreen(repairRequestNonce = repairRequestNonce)
                1 -> HistoryScreen()
                2 -> AboutScreen()
            }
        }
    }
}

internal fun resolveSelectedTab(intent: Intent?): Int =
    resolveSelectedTab(
        action = intent?.action,
        tab = intent?.getStringExtra(MainActivity.EXTRA_TAB),
    )

internal fun resolveSelectedTab(
    action: String?,
    tab: String?,
): Int =
    when {
        action == MainActivity.ACTION_OPEN_PROTECTION -> 0
        tab == MainActivity.TAB_PROTECTION -> 0
        tab == "protecao" -> 0
        tab == MainActivity.TAB_ABOUT -> 2
        else -> 0
    }
