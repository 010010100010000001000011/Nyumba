package com.j01000011.nyumba.presentation.receipts

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.PaymentMethod
import com.j01000011.nyumba.domain.model.Receipt
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptDetailsScreen(
    receiptId: String,
    onNavigateBack: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val receipt by viewModel.selectedReceipt.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(receiptId) {
        viewModel.loadReceiptById(receiptId)
    }

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Receipt",
                onBack = onNavigateBack,
                actions = {
                    receipt?.let { r ->
                        IconButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Nyumba receipt ${r.receiptNumber}")
                                putExtra(Intent.EXTRA_TEXT, r.asShareText())
                            }
                            context.startActivity(Intent.createChooser(send, "Share receipt"))
                        }) { Icon(Icons.Default.Share, contentDescription = "Share receipt") }
                    }
                }
            )
        }
    ) { padding ->
        val r = receipt
        when {
            uiState is ReceiptUiState.Error -> Box(Modifier.padding(padding).padding(20.dp)) {
                InlineMessage(text = (uiState as ReceiptUiState.Error).message, icon = Icons.Default.ErrorOutline)
            }
            r == null -> LoadingState(Modifier.padding(padding))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeroCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Payment received", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(formatKwacha(r.amountPaid), style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            SimpleDateFormat("EEEE, d MMMM yyyy · HH:mm", Locale.getDefault()).format(Date(r.paymentDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                SurfaceCard {
                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Receipt no.", r.receiptNumber)
                    InfoRow("Tenant", r.tenantName)
                    InfoRow("Room", r.roomName)
                    InfoRow("Method", if (r.paymentMethod == PaymentMethod.MOBILE_MONEY) "Mobile money" else "Cash")
                    r.transactionId?.takeIf { it.isNotBlank() }?.let { InfoRow("Transaction ID", it) }
                }

                SurfaceCard {
                    Text("Balance", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    if (r.previousBalance > 0) InfoRow("Before this payment", formatKwacha(r.previousBalance))
                    InfoRow("Amount paid", formatKwacha(r.amountPaid))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    InfoRow("Remaining", formatKwacha(r.remainingBalance))
                }

                val settled = r.remainingBalance <= 0
                InlineMessage(
                    text = if (settled) "Rent fully paid. Thank you!" else "${formatKwacha(r.remainingBalance)} still outstanding on your rent schedule.",
                    icon = if (settled) Icons.Default.CheckCircle else Icons.Default.Info,
                    tone = if (settled) Tone.Success else Tone.Warning
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Text(
                        "Issued by Nyumba on behalf of your landlord. Keep this receipt for your records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun Receipt.asShareText(): String = buildString {
    val date = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(paymentDate))
    appendLine("NYUMBA RENT RECEIPT")
    appendLine("Receipt no: $receiptNumber")
    appendLine("Date: $date")
    appendLine("Tenant: $tenantName")
    appendLine("Room: $roomName")
    appendLine("Amount paid: ${formatKwacha(amountPaid)}")
    appendLine("Method: ${if (paymentMethod == PaymentMethod.MOBILE_MONEY) "Mobile money" else "Cash"}")
    transactionId?.takeIf { it.isNotBlank() }?.let { appendLine("Transaction ID: $it") }
    append("Remaining balance: ${formatKwacha(remainingBalance)}")
}
