package com.smsguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsguard.core.HistoryEvent
import com.smsguard.core.RiskLevel
import com.smsguard.storage.HistoryStore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val historyStore = remember { HistoryStore(context) }
    val events = remember { historyStore.getAllEvents() }

    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No suspicious events detected yet.", fontSize = 20.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Detection History", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(events) { event ->
                HistoryItem(event)
            }
        }
    }
}

@Composable
fun HistoryItem(event: HistoryEvent) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val date = sdf.format(Date(event.timestamp))

    val riskColor = when (event.riskLevel) {
        RiskLevel.HIGH -> Color(0xFFC62828)
        RiskLevel.MEDIUM -> Color(0xFFF57C00)
        RiskLevel.LOW -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = event.sender, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Box(
                    modifier = Modifier
                        .background(riskColor, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = event.riskLevel.name, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Domain: ${event.domain}", fontSize = 18.sp)
            Text(text = date, fontSize = 14.sp, color = Color.Gray)
        }
    }
}
