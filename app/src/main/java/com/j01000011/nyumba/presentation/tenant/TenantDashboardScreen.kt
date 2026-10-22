package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.domain.model.RoomRequestStatus
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.payments.tone
import com.j01000011.nyumba.ui.components.*
import com.j01000011.nyumba.ui.theme.HeroGradientStart
import java.text.SimpleDateFormat
import java.util.*

private data class TenantAction(val icon: ImageVector, val label: String, val tone: Tone, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToRoomDiscovery: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToRent: () -> Unit,
    onNavigateToMakePayment: () -> Unit,
    onNavigateToReceipts: () -> Unit,
    onNavigateToPaymentHistory: () -> Unit,
    onNavigateToMyRoom: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToProfile: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: TenantDashboardViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val stats by dashboardViewModel.stats.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let { dashboardViewModel.loadDashboardData(it) }
    }

    val firstName = user?.fullName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() } ?: "there"
    val hasRoom = user?.currentRoomId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${greeting()}, $firstName", style = MaterialTheme.typography.titleLarge)
                        Text(
                            stats.currentHouse?.name ?: "Welcome to Nyumba",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Account")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                onClick = { menuOpen = false; onNavigateToProfile() }
                            )
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                                onClick = {
                                    menuOpen = false
                                    authViewModel.logout()
                                    onLogout()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasRoom) {
                item {
                    SurfaceCard {
                        EmptyState(
                            icon = Icons.Default.TravelExplore,
                            title = "Find your next room",
                            message = "Browse boarding houses near you and request a room. Your rent and receipts will show up here once you're approved.",
                            actionLabel = "Browse boarding houses",
                            onAction = onNavigateToRoomDiscovery,
                            modifier = Modifier.padding(0.dp)
                        )
                    }
                }
                item {
                    GridRows(
                        items = listOf(
                            TenantAction(Icons.AutoMirrored.Filled.Message, "Messages", Tone.Info, onNavigateToMessages),
                            TenantAction(Icons.Default.Person, "Profile", Tone.Neutral, onNavigateToProfile)
                        ),
                        columns = 2
                    ) { action, modifier ->
                        QuickAction(action.icon, action.label, action.onClick, modifier, action.tone)
                    }
                }
            } else {
                item {
                    BalanceHero(
                        balance = stats.outstandingBalance,
                        nextDueDate = stats.nextDueDate,
                        nextDueStatus = stats.nextDueStatus,
                        advanceMonths = stats.advancePeriodsCovered,
                        onPay = onNavigateToMakePayment,
                        onViewLedger = onNavigateToRent
                    )
                }
                item {
                    SurfaceCard(onClick = onNavigateToMyRoom) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Default.MeetingRoom, tone = Tone.Brand)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stats.currentRoom?.roomName ?: "My room", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stats.currentHouse?.address?.ifBlank { null } ?: stats.currentHouse?.name ?: "Boarding house",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    SectionHeader("Quick actions")
                    GridRows(
                        items = listOf(
                            TenantAction(Icons.Default.CalendarMonth, "Rent", Tone.Brand, onNavigateToRent),
                            TenantAction(Icons.AutoMirrored.Filled.ReceiptLong, "Receipts", Tone.Brand, onNavigateToReceipts),
                            TenantAction(Icons.Default.History, "History", Tone.Info, onNavigateToPaymentHistory),
                            TenantAction(Icons.Default.ReportProblem, "Complaints", Tone.Warning, onNavigateToComplaints),
                            TenantAction(Icons.AutoMirrored.Filled.Message, "Messages", Tone.Info, onNavigateToMessages),
                            TenantAction(Icons.Default.MeetingRoom, "My room", Tone.Neutral, onNavigateToMyRoom)
                        ),
                        columns = 3
                    ) { action, modifier ->
                        QuickAction(action.icon, action.label, action.onClick, modifier, action.tone)
                    }
                }
            }

            if (stats.requests.isNotEmpty()) {
                item { SectionHeader("Room requests") }
                items(stats.requests, key = { it.requestId }) { request ->
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Default.Bookmark, tone = request.status.tone(), size = 40)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Room request", style = MaterialTheme.typography.titleSmall)
                                val detail = when {
                                    request.status == RoomRequestStatus.HOLDING && request.holdUntil != null -> {
                                        val mins = maxOf(0, (request.holdUntil - System.currentTimeMillis()) / 60_000)
                                        "Held for you · expires in ${formatDuration(mins)}"
                                    }
                                    else -> "Requested ${SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(request.createdAt))}"
                                }
                                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TenantStatusChip(request.status)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BalanceHero(
    balance: Double,
    nextDueDate: Long?,
    nextDueStatus: RentStatus?,
    advanceMonths: Int,
    onPay: () -> Unit,
    onViewLedger: () -> Unit
) {
    HeroCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Balance due", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
            if (nextDueStatus == RentStatus.OVERDUE) {
                Surface(color = Color.White, shape = MaterialTheme.shapes.small) {
                    Text("Overdue", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(formatKwacha(balance), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(4.dp))
        val dueText = nextDueDate?.let { "Next due ${SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(it))}" } ?: "You're all paid up"
        Text(dueText, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.85f))
        if (advanceMonths > 0) {
            Text("$advanceMonths month(s) paid in advance", style = MaterialTheme.typography.bodySmall, color = LocalContentColor.current.copy(alpha = 0.75f))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onPay,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = HeroGradientStart)
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pay now")
            }
            OutlinedButton(
                onClick = onViewLedger,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text("View ledger") }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

private fun formatDuration(minutes: Long): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"

fun RoomRequestStatus.tone(): Tone = when (this) {
    RoomRequestStatus.APPROVED -> Tone.Success
    RoomRequestStatus.PENDING -> Tone.Info
    RoomRequestStatus.HOLDING -> Tone.Warning
    RoomRequestStatus.REJECTED, RoomRequestStatus.EXPIRED -> Tone.Danger
}

@Composable
fun TenantStatusChip(status: RoomRequestStatus) {
    StatusPill(text = humanize(status.name), tone = status.tone())
}
