package com.j01000011.nyumba.presentation.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Payment
import com.j01000011.nyumba.domain.model.PaymentMethod
import com.j01000011.nyumba.presentation.landlord.TenantManagementViewModel
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LandlordPaymentsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRecordCash: (String) -> Unit,
    rentViewModel: RentViewModel = hiltViewModel(),
    tenantViewModel: TenantManagementViewModel = hiltViewModel()
) {
    val payments by rentViewModel.paymentHistory.collectAsState()
    val tenants by tenantViewModel.tenants.collectAsState()
    var methodFilter by remember { mutableStateOf<PaymentMethod?>(null) }

    LaunchedEffect(houseId) {
        rentViewModel.loadLandlordPayments(houseId)
        tenantViewModel.loadTenants(houseId)
    }

    val tenantNames = remember(tenants) { tenants.associate { it.uid to it.fullName } }
    val filtered = payments.filter { methodFilter == null || it.paymentMethod == methodFilter }
    val monthStart = remember {
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
    }
    val thisMonth = payments.filter { it.paymentDate >= monthStart }.sumOf { it.amount }

    Scaffold(
        topBar = { NyumbaTopBar(title = "Payments", onBack = onNavigateBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToRecordCash(houseId) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Record cash") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HeroCard {
                    Text("Collected this month", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.8f))
                    Text(formatKwacha(thisMonth), style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${payments.size} payment(s) · ${formatKwacha(payments.sumOf { it.amount })} all time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.85f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    FilterChip(selected = methodFilter == null, onClick = { methodFilter = null }, label = { Text("All") })
                    FilterChip(selected = methodFilter == PaymentMethod.MOBILE_MONEY, onClick = { methodFilter = PaymentMethod.MOBILE_MONEY }, label = { Text("Mobile money") })
                    FilterChip(selected = methodFilter == PaymentMethod.CASH, onClick = { methodFilter = PaymentMethod.CASH }, label = { Text("Cash") })
                }
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Payments,
                        title = "No payments yet",
                        message = "Mobile-money payments appear here automatically. Record cash payments with the button below."
                    )
                }
            } else {
                items(filtered, key = { it.paymentId }) { payment ->
                    PaymentRow(payment, title = tenantNames[payment.tenantId] ?: "Former tenant")
                }
            }
        }
    }
}

/** A single payment in a list; [title] is the tenant name (landlord view) or a description (tenant view). */
@Composable
fun PaymentRow(payment: Payment, title: String) {
    val isMobile = payment.paymentMethod == PaymentMethod.MOBILE_MONEY
    val date = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()).format(Date(payment.paymentDate))
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (isMobile) Icons.Default.PhoneAndroid else Icons.Default.Payments, tone = if (isMobile) Tone.Info else Tone.Success)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "$date · ${if (isMobile) "Mobile money" else "Cash"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                payment.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(formatKwacha(payment.amount), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
