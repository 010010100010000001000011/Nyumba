package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.RoomRequestStatus
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToRoomDiscovery: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToRent: () -> Unit,
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

    LaunchedEffect(user) {
        user?.let {
            dashboardViewModel.loadDashboardData(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nyumba - Tenant") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Welcome, ${user?.fullName ?: "Tenant"}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (user?.currentRoomId == null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "You are currently not assigned to a room.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onNavigateToRoomDiscovery,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Find a Room")
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Financial Overview",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Balance Due", style = MaterialTheme.typography.labelSmall)
                                    Text("K${String.format(Locale.getDefault(), "%.2f", stats.outstandingBalance)}", style = MaterialTheme.typography.titleLarge)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Next Due", style = MaterialTheme.typography.labelSmall)
                                    val dateStr = stats.nextDueDate?.let { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(it)) } ?: "N/A"
                                    Text(dateStr, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                            
                            if (stats.advancePeriodsCovered > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Advance Credit: ${stats.advancePeriodsCovered} months covered",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stats.currentRoom?.roomName ?: "Room", style = MaterialTheme.typography.titleSmall)
                                Text(stats.currentHouse?.name ?: "Boarding House", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardActionTile(
                            onClick = onNavigateToMyRoom,
                            icon = Icons.Default.MeetingRoom,
                            label = "My Room",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionTile(
                            onClick = onNavigateToRent,
                            icon = Icons.Default.Payments,
                            label = "Rent",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardActionTile(
                            onClick = onNavigateToReceipts,
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            label = "Receipts",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionTile(
                            onClick = onNavigateToPaymentHistory,
                            icon = Icons.Default.History,
                            label = "History",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardActionTile(
                            onClick = onNavigateToComplaints,
                            icon = Icons.Default.Warning,
                            label = "Complaints",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardActionTile(
                            onClick = onNavigateToMessages,
                            icon = Icons.AutoMirrored.Filled.Message,
                            label = "Messages",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (stats.requests.isNotEmpty()) {
                item {
                    Text("My Requests & Holds", style = MaterialTheme.typography.titleLarge)
                }
                
                items(stats.requests) { request ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Room Request", style = MaterialTheme.typography.titleMedium)
                                TenantStatusChip(request.status)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (request.status == RoomRequestStatus.HOLDING) {
                                request.holdUntil?.let {
                                    val remaining = (it - System.currentTimeMillis()) / 1000 / 60
                                    Text(
                                        text = "Hold expires in: ${maxOf(0, remaining)} mins",
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionTile(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun TenantStatusChip(status: RoomRequestStatus) {
    val color = when (status) {
        RoomRequestStatus.APPROVED -> MaterialTheme.colorScheme.primary
        RoomRequestStatus.PENDING, RoomRequestStatus.HOLDING -> MaterialTheme.colorScheme.secondary
        RoomRequestStatus.REJECTED, RoomRequestStatus.EXPIRED -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
