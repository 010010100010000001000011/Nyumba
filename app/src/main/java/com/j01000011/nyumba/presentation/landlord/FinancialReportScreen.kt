package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Payment
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialReportScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val report by viewModel.report.collectAsState()
    
    LaunchedEffect(houseId) {
        viewModel.loadReportData(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Collected", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "K${"%.2f".format(report.totalCollected)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ReportStatCard("Expected", "K${"%.0f".format(report.totalExpected)}", Modifier.weight(1f))
                    ReportStatCard("Outstanding", "K${"%.0f".format(report.totalOutstanding)}", Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ReportStatCard("Overdue Periods", report.overdueCount.toString(), Modifier.weight(1f))
                    ReportStatCard("Advance Payments", report.advancePaidCount.toString(), Modifier.weight(1f))
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Financial Overview", style = MaterialTheme.typography.titleLarge)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "This report summarizes the financial health of the selected boarding house based on current rent cycle data and payment history.", 
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recent Payments", style = MaterialTheme.typography.titleLarge)
            }

            if (report.recentPayments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No recent payments recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(report.recentPayments) { payment ->
                    ReportPaymentItemCard(payment)
                }
            }
        }
    }
}

@Composable
fun ReportPaymentItemCard(payment: Payment) {
    val dateStr = remember(payment.paymentDate) {
        SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()).format(Date(payment.paymentDate))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "K${payment.amount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!payment.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Notes: ${payment.notes}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = payment.paymentMethod.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ReportStatCard(
    label: String, 
    value: String, 
    modifier: Modifier = Modifier, 
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
