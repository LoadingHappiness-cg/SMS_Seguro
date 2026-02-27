package com.smsguard.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smsguard.core.HistoryEvent
import com.smsguard.core.AlertType
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
            Text(stringResource(com.smsguard.R.string.history_empty), fontSize = 20.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(com.smsguard.R.string.history_detection_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(events) { event ->
                HistoryItem(
                    event = event,
                    onOpenDetails = { openAlertFromHistory(context, event) },
                )
            }
        }
    }
}

@Composable
fun HistoryItem(
    event: HistoryEvent,
    onOpenDetails: () -> Unit,
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val date = sdf.format(Date(event.timestamp))

    val riskColor = when (event.riskLevel) {
        RiskLevel.HIGH -> Color(0xFFC62828)
        RiskLevel.MEDIUM -> Color(0xFFF57C00)
        RiskLevel.LOW -> Color(0xFF2E7D32)
    }

    val riskLabel =
        when (event.riskLevel) {
            RiskLevel.HIGH -> stringResource(com.smsguard.R.string.risk_high)
            RiskLevel.MEDIUM -> stringResource(com.smsguard.R.string.risk_medium)
            RiskLevel.LOW -> stringResource(com.smsguard.R.string.risk_low)
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenDetails,
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
                    Text(text = riskLabel, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            when (event.alertType) {
                AlertType.MULTIBANCO -> {
                    val entidade = event.multibancoEntidade.orEmpty()
                    val referencia = event.multibancoReferencia.orEmpty()
                    val valor = event.multibancoValor

                    Text(
                        text = stringResource(com.smsguard.R.string.mb_payment_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (entidade.isNotBlank()) {
                        Text(
                            text = "${stringResource(com.smsguard.R.string.mb_entity)}: $entidade",
                            fontSize = 16.sp
                        )
                    }
                    if (referencia.isNotBlank()) {
                        Text(
                            text = "${stringResource(com.smsguard.R.string.mb_reference)}: $referencia",
                            fontSize = 16.sp
                        )
                    }
                    if (!valor.isNullOrBlank()) {
                        Text(
                            text = "${stringResource(com.smsguard.R.string.mb_amount)}: $valor€",
                            fontSize = 16.sp
                        )
                    }
                }
                AlertType.URL -> {
                    val domain = event.domain.ifBlank { stringResource(com.smsguard.R.string.unknown) }
                    Text(text = stringResource(com.smsguard.R.string.history_domain, domain), fontSize = 18.sp)
                }
            }
            Text(text = date, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

private fun openAlertFromHistory(context: Context, event: HistoryEvent) {
    val intent =
        Intent(context, AlertActivity::class.java).apply {
            putExtra("sender", event.sender)
            putExtra("domain", event.domain)
            putExtra("url", event.url.orEmpty())
            putExtra("score", event.score)
            putExtra("level", event.riskLevel.name)
            putExtra("alert_type", event.alertType.name)
            putStringArrayListExtra("reasons", ArrayList(event.reasons))
            putExtra("mb_entidade", event.multibancoEntidade.orEmpty())
            putExtra("mb_referencia", event.multibancoReferencia.orEmpty())
            putExtra("mb_valor", event.multibancoValor)
        }
    context.startActivity(intent)
}
