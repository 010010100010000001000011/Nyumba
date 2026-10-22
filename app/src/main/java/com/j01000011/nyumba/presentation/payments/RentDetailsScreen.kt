package com.j01000011.nyumba.presentation.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

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

    val hasUnpaid = periods.any { it.status != RentStatus.PAID && it.status != RentStatus.ADVANCE_PAID }

    Scaffold(
        topBar = { NyumbaTopBar(title = "Rent ledger", onBack = onNavigateBack) },
        bottomBar = {
            if (hasUnpaid) {
                Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                    PrimaryButton(
                        text = "Pay rent",
                        onClick = onNavigateToMakePayment,
                        icon = Icons.Default.Payments,
                        modifier = Modifier.navigationBarsPadding().padding(16.dp)
                    )
                }
            }
        }
    ) { padding ->
        if (periods.isEmpty()) {
            EmptyState(
                icon = Icons.Default.CalendarMonth,
                title = "No rent records yet",
                message = "Your rent schedule appears here once a landlord approves your room.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SummaryCard(periods) }
                item { SectionHeader("Monthly schedule") }
                items(periods, key = { it.periodId }) { period -> RentPeriodCard(period) }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun SummaryCard(periods: List<RentPeriod>) {
    val due = periods.filter { it.isCurrentOrPastDue && it.status != RentStatus.PAID && it.status != RentStatus.ADVANCE_PAID }
    val totalDue = due.sumOf { it.remainingBalance }
    val totalPaid = periods.sumOf { it.amountPaid }
    val overdueCount = periods.count { it.status == RentStatus.OVERDUE }
    HeroCard {
        Text("Outstanding balance", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.8f))
        Spacer(Modifier.height(4.dp))
        Text(formatKwacha(totalDue), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Paid to date", style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f))
                Text(formatKwacha(totalPaid), style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Overdue months", style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f))
                Text(overdueCount.toString(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun RentPeriodCard(period: RentPeriod) {
    val monthName = remember(period.month) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(
            Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.MONTH, period.month - 1) }.time
        )
    }
    val progress = if (period.amountDue > 0) (period.amountPaid / period.amountDue).toFloat().coerceIn(0f, 1f) else 0f
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$monthName ${period.year}", style = MaterialTheme.typography.titleMedium)
                if (period.dueDate > 0) {
                    Text(
                        "Due ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(period.dueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RentStatusChip(period.status)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            strokeCap = StrokeCap.Round,
            color = if (period.status == RentStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                "${formatKwacha(period.amountPaid)} of ${formatKwacha(period.amountDue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (period.remainingBalance > 0) {
                Text(
                    "${formatKwacha(period.remainingBalance)} left",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (period.status == RentStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun RentStatus.tone(): Tone = when (this) {
    RentStatus.PAID -> Tone.Success
    RentStatus.ADVANCE_PAID -> Tone.Info
    RentStatus.PARTIALLY_PAID -> Tone.Warning
    RentStatus.UNPAID -> Tone.Neutral
    RentStatus.OVERDUE -> Tone.Danger
}

@Composable
fun RentStatusChip(status: RentStatus) {
    StatusPill(text = humanize(status.name), tone = status.tone())
}
