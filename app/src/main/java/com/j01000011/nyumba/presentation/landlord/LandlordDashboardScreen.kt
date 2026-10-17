package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.BoardingHouse
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordDashboardScreen(
    onLogout: () -> Unit,
    onNavigateToRooms: (String) -> Unit,
    onNavigateToAddBoardingHouse: () -> Unit,
    onNavigateToEditHouse: (String) -> Unit,
    onNavigateToRoomRequests: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToPayments: (String) -> Unit,
    onNavigateToTenants: (String) -> Unit,
    onNavigateToComplaints: (String) -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToReports: (String) -> Unit,
    onNavigateToAuditLogs: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBroadcast: (String) -> Unit,
    onNavigateToHouseReceipts: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    switcherViewModel: BoardingHouseSwitcherViewModel = hiltViewModel(),
    dashboardViewModel: LandlordDashboardViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val boardingHouses by switcherViewModel.boardingHouses.collectAsState()
    val activeHouse by switcherViewModel.activeBoardingHouse.collectAsState()
    val stats by dashboardViewModel.stats.collectAsState()
    
    var showSwitcher by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let { switcherViewModel.loadBoardingHouses(it.uid) }
    }

    LaunchedEffect(activeHouse) {
        activeHouse?.let { dashboardViewModel.loadStats(it.boardingHouseId) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nyumba Landlord", style = MaterialTheme.typography.titleMedium)
                        activeHouse?.let {
                            Text(it.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                    IconButton(onClick = { showSwitcher = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch House")
                    }
                    IconButton(onClick = {
                        try {
                            authViewModel.logout()
                        } catch (_: Exception) {
                            // Handle exception gracefully
                        } finally {
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddBoardingHouse) {
                Icon(Icons.Default.Add, contentDescription = "Add Boarding House")
            }
        }
    ) { padding ->
        if (boardingHouses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No boarding houses managed yet.")
                    Button(onClick = onNavigateToAddBoardingHouse, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Add Your First House")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                item {
                    FinancialOverviewCard(stats)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    OperationalSummaryCard(stats)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text("Management", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToRooms(it.boardingHouseId) } },
                            icon = Icons.Default.Home,
                            label = "Rooms",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToRoomRequests(it.boardingHouseId) } },
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            label = "Requests",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToPayments(it.boardingHouseId) } },
                            icon = Icons.Default.Payments,
                            label = "Payments",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToHouseReceipts(it.boardingHouseId) } },
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            label = "Receipts",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToTenants(it.boardingHouseId) } },
                            icon = Icons.Default.People,
                            label = "Tenants",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToComplaints(it.boardingHouseId) } },
                            icon = Icons.Default.Warning,
                            label = "Complaints",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DashboardButton(
                        onClick = onNavigateToMessages,
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "Messages",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Communication", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { activeHouse?.let { onNavigateToBroadcast(it.boardingHouseId) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Announcement, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Announcement")
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Insights & Reports", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToReports(it.boardingHouseId) } },
                            icon = Icons.Default.Assessment,
                            label = "Finance Report",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        DashboardButton(
                            onClick = { activeHouse?.let { onNavigateToAuditLogs(it.boardingHouseId) } },
                            icon = Icons.Default.History,
                            label = "Audit Logs",
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }


                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("House Details", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { activeHouse?.let { onNavigateToEditHouse(it.boardingHouseId) } }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    activeHouse?.let { house ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Address: ${house.address}")
                                Text("Phone: ${house.phone}")
                                if (house.description.isNotBlank()) {
                                    Text("Description: ${house.description}")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSwitcher) {
            BoardingHouseSwitcherDialog(
                houses = boardingHouses,
                onDismiss = { showSwitcher = false },
                onSelect = {
                    switcherViewModel.switchBoardingHouse(it)
                    showSwitcher = false
                }
            )
        }
    }
}



@Composable
fun FinancialOverviewCard(stats: com.j01000011.nyumba.presentation.landlord.DashboardStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Revenue cycle overview", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Collected", style = MaterialTheme.typography.labelSmall)
                    Text("K${"%.2f".format(stats.rentCollected)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Outstanding", style = MaterialTheme.typography.labelSmall)
                    Text("K${"%.2f".format(stats.outstandingRent)}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${stats.advancePaidTenants} Tenants have advance credit coverage", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun OperationalSummaryCard(stats: com.j01000011.nyumba.presentation.landlord.DashboardStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LandlordStatItem("Occupancy", "${stats.occupiedSpaces}/${stats.totalCapacity}")
            LandlordStatItem("Pending Req.", stats.pendingRequests.toString())
            LandlordStatItem("Overdue Comp.", stats.overdueComplaints.toString(), isError = stats.overdueComplaints > 0)
        }
    }
}

@Composable
fun LandlordStatItem(label: String, value: String, isError: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, 
            style = MaterialTheme.typography.titleMedium, 
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun DashboardButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BoardingHouseSwitcherDialog(
    houses: List<BoardingHouse>,
    onDismiss: () -> Unit,
    onSelect: (BoardingHouse) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Boarding House") },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                LazyColumn {
                    items(houses) { house ->
                        ListItem(
                            headlineContent = { Text(house.name) },
                            supportingContent = { Text(house.address) },
                            modifier = Modifier.clickable { onSelect(house) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
