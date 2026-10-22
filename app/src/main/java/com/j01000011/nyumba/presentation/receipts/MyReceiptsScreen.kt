package com.j01000011.nyumba.presentation.receipts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.PaymentMethod
import com.j01000011.nyumba.domain.model.Receipt
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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

    ReceiptListScaffold(
        title = "My receipts",
        receipts = receipts,
        emptyMessage = "Receipts are created automatically every time you pay rent.",
        onNavigateBack = onNavigateBack,
        onOpen = onNavigateToReceiptDetails,
        showTenant = false
    )
}

/** Shared by the tenant and landlord receipt lists. */
@Composable
internal fun ReceiptListScaffold(
    title: String,
    receipts: List<Receipt>,
    emptyMessage: String,
    onNavigateBack: () -> Unit,
    onOpen: (String) -> Unit,
    showTenant: Boolean
) {
    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = title,
                subtitle = if (receipts.isEmpty()) null else "${receipts.size} receipt(s) · ${formatKwacha(receipts.sumOf { it.amountPaid })}",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        if (receipts.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "No receipts yet",
                message = emptyMessage,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(receipts, key = { it.receiptId }) { receipt ->
                    ReceiptItemCard(receipt = receipt, showTenant = showTenant, onClick = { onOpen(receipt.receiptId) })
                }
            }
        }
    }
}

@Composable
fun ReceiptItemCard(receipt: Receipt, onClick: () -> Unit, showTenant: Boolean = false) {
    val dateStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(receipt.paymentDate))
    val isMobile = receipt.paymentMethod == PaymentMethod.MOBILE_MONEY
    SurfaceCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (isMobile) Icons.Default.PhoneAndroid else Icons.Default.Payments, tone = Tone.Success)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (showTenant) receipt.tenantName else receipt.receiptNumber,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    listOfNotNull(
                        dateStr,
                        if (isMobile) "Mobile money" else "Cash",
                        receipt.roomName.takeIf { showTenant }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatKwacha(receipt.amountPaid), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
