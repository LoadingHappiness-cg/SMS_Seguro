package com.smsguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.smsguard.core.PermissionHealth
import com.smsguard.ui.theme.SMSGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SMSGuardTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var isProtectionReady by remember { mutableStateOf(PermissionHealth(context).isProtectionReady()) }

    fun refreshProtectionReady() {
        isProtectionReady = PermissionHealth(context).isProtectionReady()
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
        refreshProtectionReady()
        if (!isProtectionReady) {
            selectedTab = 0
        }
    }

    Scaffold(
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
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(com.smsguard.R.string.cd_setup)) },
                    label = { Text(stringResource(com.smsguard.R.string.tab_setup)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    enabled = isProtectionReady,
                    icon = { Icon(Icons.Default.History, contentDescription = stringResource(com.smsguard.R.string.cd_history)) },
                    label = { Text(stringResource(com.smsguard.R.string.tab_history)) }
                )
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> {
                    if (isProtectionReady) {
                        SetupScreen()
                    } else {
                        SetupPermissionsScreen(
                            onContinue = {
                                refreshProtectionReady()
                                if (isProtectionReady) {
                                    selectedTab = 0
                                }
                            },
                        )
                    }
                }
                1 -> HistoryScreen()
            }
        }
    }
}
