package com.smsguard.ui

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.smsguard.rules.RuleLoader
import com.smsguard.update.RuleUpdateWorker

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val ruleLoader = remember { RuleLoader(context) }
    val currentRules = remember { ruleLoader.loadCurrent() }
    
    var isNotificationEnabled by remember { 
        mutableStateOf(isNotificationServiceEnabled(context)) 
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
            text = "SMSGuard",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isNotificationEnabled) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isNotificationEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isNotificationEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isNotificationEnabled) "Protection Active" else "Protection Disabled",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                
                if (!isNotificationEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Text("Enable Notification Access", fontSize = 18.sp)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ruleset Information", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Version: ${currentRules.version}", fontSize = 18.sp)
                Text("Last Update: ${currentRules.publishedAt}", fontSize = 18.sp)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        val request = OneTimeWorkRequestBuilder<RuleUpdateWorker>().build()
                        WorkManager.getInstance(context).enqueue(request)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Updates Now", fontSize = 18.sp)
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
                    text = "This application analyzes SMS notifications locally and never sends message content to the internet.",
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(pkgName) == true
}
