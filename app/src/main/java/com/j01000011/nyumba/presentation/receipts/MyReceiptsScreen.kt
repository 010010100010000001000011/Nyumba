package com.j01000011.nyumba.presentation.receipts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Receipt
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReceiptsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToReceiptDetails: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    receiptViewModel: ReceiptViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val receipts by receiptViewModel.receipts.collectAsState()

    LaunchedEffect(user) {
        user?.let { receiptViewModel.loadTenantReceipts(it.uid) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Receipts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (receipts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No receipts found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(receipts) { receipt ->
                    ReceiptItemCard(
                        receipt = receipt,
                        onClick = { onNavigateToReceiptDetails(receipt.receiptId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptItemCard(receipt: Receipt, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(receipt.paymentDate))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(receipt.receiptNumber, style = MaterialTheme.typography.titleMedium)
                Text("K${receipt.amountPaid}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Date: $dateStr", style = MaterialTheme.typography.bodySmall)
            Text("Method: ${receipt.paymentMethod.name}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
