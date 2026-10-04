package com.j01000011.nyumba.presentation.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Payment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordPaymentsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRecordCash: (String) -> Unit,
    rentViewModel: RentViewModel = hiltViewModel()
) {
    val payments by rentViewModel.paymentHistory.collectAsState()

    LaunchedEffect(houseId) {
        rentViewModel.loadLandlordPayments(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payments History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToRecordCash(houseId) }) {
                Icon(Icons.Default.Add, contentDescription = "Record Cash Payment")
            }
        }
    ) { padding ->
        if (payments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No payment records found for this house.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(payments) { payment ->
                    PaymentItemCard(payment)
                }
            }
        }
    }
}

@Composable
fun PaymentItemCard(payment: Payment) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(payment.paymentDate))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("K${payment.amount}", style = MaterialTheme.typography.titleLarge)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = payment.paymentMethod.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tenant ID: ${payment.tenantId}", style = MaterialTheme.typography.bodySmall)
            Text("Date: $dateStr", style = MaterialTheme.typography.bodySmall)
            if (!payment.notes.isNullOrBlank()) {
                Text("Notes: ${payment.notes}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
