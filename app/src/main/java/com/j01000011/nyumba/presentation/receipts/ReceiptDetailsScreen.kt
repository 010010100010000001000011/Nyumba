package com.j01000011.nyumba.presentation.receipts

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailsScreen(
    receiptId: String,
    onNavigateBack: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val receipt by viewModel.selectedReceipt.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(receiptId) {
        viewModel.loadReceiptById(receiptId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState is ReceiptUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState is ReceiptUiState.Error) {
                Text(
                    text = (uiState as ReceiptUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                receipt?.let { r ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "OFFICIAL RECEIPT",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Nyumba Management",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                ReceiptRow("Receipt No:", r.receiptNumber)
                                ReceiptRow("Date:", SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(r.paymentDate)))
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                
                                ReceiptRow("Tenant Name:", r.tenantName)
                                ReceiptRow("Room:", r.roomName)
                                ReceiptRow("Payment Method:", r.paymentMethod.name)
                                
                                if (!r.transactionId.isNullOrBlank()) {
                                    ReceiptRow("Transaction ID:", r.transactionId)
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "Amount Paid",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "K${String.format(Locale.getDefault(), "%.2f", r.amountPaid)}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                if (r.remainingBalance > 0) {
                                    Text(
                                        text = "Remaining Balance: K${String.format(Locale.getDefault(), "%.2f", r.remainingBalance)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        text = "RENT FULLY PAID",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "Thank you for your payment!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
