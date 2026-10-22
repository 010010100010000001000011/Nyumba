package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.payments.PaymentRow
import com.j01000011.nyumba.ui.components.*

@Composable
fun FinancialReportScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
    tenantViewModel: TenantManagementViewModel = hiltViewModel()
) {
    val report by viewModel.report.collectAsState()
    val tenants by tenantViewModel.tenants.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadReportData(houseId)
        tenantViewModel.loadTenants(houseId)
    }

    val tenantNames = remember(tenants) { tenants.associate { it.uid to it.fullName } }
    val collectionRate = if (report.totalExpected > 0) {
        ((report.totalExpected - report.totalOutstanding) / report.totalExpected).toFloat().coerceIn(0f, 1f)
    } else 0f

    Scaffold(topBar = { NyumbaTopBar(title = "Financial report", onBack = onNavigateBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeroCard {
                    Text("Total collected", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.8f))
                    Text(formatKwacha(report.totalCollected), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Collection rate ${(collectionRate * 100).toInt()}% of rent due to date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { collectionRate },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = StrokeCap.Round,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        drawStopIndicator = {}
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricTile("Due to date", formatKwacha(report.totalExpected), Icons.Default.CalendarMonth, Modifier.weight(1f), Tone.Info)
                    MetricTile(
                        "Outstanding", formatKwacha(report.totalOutstanding), Icons.Default.HourglassBottom, Modifier.weight(1f),
                        if (report.totalOutstanding > 0) Tone.Danger else Tone.Success
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricTile(
                        "Overdue months", report.overdueCount.toString(), Icons.Default.Warning, Modifier.weight(1f),
                        if (report.overdueCount > 0) Tone.Warning else Tone.Neutral
                    )
                    MetricTile("Paid in advance", report.advancePaidCount.toString(), Icons.Default.Savings, Modifier.weight(1f), Tone.Success)
                }
            }
            item { SectionHeader("Recent payments") }
            if (report.recentPayments.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Payments,
                        title = "No payments yet",
                        message = "Payments for this house will show up here."
                    )
                }
            } else {
                items(report.recentPayments.take(20), key = { it.paymentId }) { payment ->
                    PaymentRow(payment, title = tenantNames[payment.tenantId] ?: "Former tenant")
                }
            }
        }
    }
}
