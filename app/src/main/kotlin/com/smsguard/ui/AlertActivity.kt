package com.smsguard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsguard.ui.theme.SMSGuardTheme

class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val riskLevel = intent.getStringExtra("risk_level") ?: "UNKNOWN"
        val domain = intent.getStringExtra("domain") ?: ""
        val reasons = intent.getStringArrayExtra("reasons") ?: emptyArray()
        val url = intent.getStringExtra("url") ?: ""

        setContent {
            SMSGuardTheme {
                AlertScreen(
                    riskLevel = riskLevel,
                    domain = domain,
                    reasons = reasons.toList(),
                    onClose = { finish() },
                    onCopy = {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("SMS Link", url)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AlertScreen(
    riskLevel: String,
    domain: String,
    reasons: List<String>,
    onClose: () -> Unit,
    onCopy: () -> Unit
) {
    val bgColor = if (riskLevel == "HIGH") Color(0xFFB71C1C) else Color(0xFFE65100)

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
            text = "DANGER DETECTED",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Risk Level: $riskLevel",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Detected Link:",
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Reasons for alert:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                reasons.forEach { reason ->
                    Text(text = "• $reason", fontSize = 18.sp, color = Color.DarkGray)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
        ) {
            Text("IGNORE & CLOSE", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
        ) {
            Text("COPY LINK", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { /* In a real app, trigger a call or help dialog */ }) {
            Text("ASK FOR HELP", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
