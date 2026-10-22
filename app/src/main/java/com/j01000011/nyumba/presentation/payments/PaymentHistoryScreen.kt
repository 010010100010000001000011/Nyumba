package com.j01000011.nyumba.presentation.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.PaymentMethod
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PaymentHistoryScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    rentViewModel: RentViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val payments by rentViewModel.paymentHistory.collectAsState()

    LaunchedEffect(user) {
        user?.let { rentViewModel.loadTenantRentData(it.uid) }
    }

    // Group by month, newest first (payments arrive sorted by date descending)
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val grouped = payments.groupBy { monthFormat.format(Date(it.paymentDate)) }

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Payment history",
                subtitle = if (payments.isEmpty()) null else "${formatKwacha(payments.sumOf { it.amount })} paid in total",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        if (payments.isEmpty()) {
            EmptyState(
                icon = Icons.Default.History,
                title = "No payments yet",
                message = "Your rent payments will be listed here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { (month, monthPayments) ->
                    item(key = month) {
                        SectionHeader("$month · ${formatKwacha(monthPayments.sumOf { it.amount })}")
                    }
                    items(monthPayments, key = { it.paymentId }) { payment ->
                        PaymentRow(
                            payment,
                            title = if (payment.paymentMethod == PaymentMethod.MOBILE_MONEY) "Rent · mobile money" else "Rent · cash"
                        )
                    }
                }
            }
        }
    }
}
