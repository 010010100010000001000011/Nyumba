package com.j01000011.nyumba.presentation.payments

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
import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentDetailsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMakePayment: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    rentViewModel: RentViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val periods by rentViewModel.rentPeriods.collectAsState()

    LaunchedEffect(user) {
        user?.let { rentViewModel.loadTenantRentData(it.uid) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rent Ledger") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (periods.any { it.status != RentStatus.PAID && it.status != RentStatus.ADVANCE_PAID }) {
                Surface(tonalElevation = 8.dp) {
                    Button(
                        onClick = onNavigateToMakePayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Make Payment")
                    }
                }
            }
        }
    ) { padding ->
        if (periods.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No rent records found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SummaryCard(periods)
                }

                items(periods) { period ->
                    RentPeriodCard(period)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(periods: List<RentPeriod>) {
    val currentOrPastPeriods = periods.filter { it.isCurrentOrPastDue }
    val totalDue = currentOrPastPeriods.filter { it.status != RentStatus.PAID && it.status != RentStatus.ADVANCE_PAID }.sumOf { maxOf(0.0, it.amountDue - it.amountPaid) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Outstanding Balance", style = MaterialTheme.typography.titleMedium)
            Text("K${String.format(Locale.getDefault(), "%.2f", totalDue)}", style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
fun RentPeriodCard(period: RentPeriod) {
    val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, period.month - 1) }.time)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$monthName ${period.year}", style = MaterialTheme.typography.titleMedium)
                RentStatusChip(period.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Amount Due: K${period.amountDue}")
                Text("Paid: K${period.amountPaid}")
            }
            if (period.amountDue > period.amountPaid) {
                Text(
                    "Remaining: K${period.amountDue - period.amountPaid}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun RentStatusChip(status: RentStatus) {
    val color = when (status) {
        RentStatus.PAID -> MaterialTheme.colorScheme.primary
        RentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.secondary
        RentStatus.UNPAID -> MaterialTheme.colorScheme.outline
        RentStatus.ADVANCE_PAID -> MaterialTheme.colorScheme.tertiary
        RentStatus.OVERDUE -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
