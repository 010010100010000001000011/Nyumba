package com.j01000011.nyumba.presentation.receipts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HouseReceiptsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToReceiptDetails: (String) -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val receipts by viewModel.receipts.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadHouseReceipts(houseId)
    }

    ReceiptListScaffold(
        title = "Receipts",
        receipts = receipts,
        emptyMessage = "Receipts appear here as tenants pay or you record cash payments.",
        onNavigateBack = onNavigateBack,
        onOpen = onNavigateToReceiptDetails,
        showTenant = true
    )
}
