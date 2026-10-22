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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.BoardingHouse
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.*

private data class ManageAction(
    val icon: ImageVector,
    val label: String,
    val tone: Tone,
    val badge: Int = 0,
    val onClick: () -> Unit
)

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
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let { switcherViewModel.loadBoardingHouses(it.uid) }
    }
    LaunchedEffect(activeHouse) {
        activeHouse?.let { dashboardViewModel.loadStats(it.boardingHouseId) }
    }

    // Runs the action against the active house, if there is one.
    fun withHouse(action: (String) -> Unit): () -> Unit = { activeHouse?.let { action(it.boardingHouseId) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        onClick = { if (boardingHouses.isNotEmpty()) showSwitcher = true },
                        shape = MaterialTheme.shapes.medium,
                        color = Color.Transparent
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text("Landlord", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    activeHouse?.name ?: "Nyumba",
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (boardingHouses.isNotEmpty()) {
                                    Icon(Icons.Default.ExpandMore, contentDescription = "Switch house")
                                }
                            }
                        }
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
                                text = { Text("Add boarding house") },
                                leadingIcon = { Icon(Icons.Default.AddHome, null) },
                                onClick = { menuOpen = false; onNavigateToAddBoardingHouse() }
                            )
                            DropdownMenuItem(
                                text = { Text("Profile") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                onClick = { menuOpen = false; onNavigateToProfile() }
                            )
                            HorizontalDivider()
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
        if (boardingHouses.isEmpty()) {
            EmptyState(
                icon = Icons.Default.AddHome,
                title = "Add your first boarding house",
                message = "Set up a house, add its rooms and start accepting tenants and rent payments.",
                actionLabel = "Add boarding house",
                onAction = onNavigateToAddBoardingHouse,
                modifier = Modifier.padding(padding).padding(top = 48.dp)
            )
        } else {
            val manageActions = listOf(
                ManageAction(Icons.Default.MeetingRoom, "Rooms", Tone.Brand, onClick = withHouse(onNavigateToRooms)),
                ManageAction(Icons.AutoMirrored.Filled.Assignment, "Requests", Tone.Info, stats.pendingRequests, withHouse(onNavigateToRoomRequests)),
                ManageAction(Icons.Default.People, "Tenants", Tone.Brand, onClick = withHouse(onNavigateToTenants)),
                ManageAction(Icons.Default.Payments, "Payments", Tone.Success, onClick = withHouse(onNavigateToPayments)),
                ManageAction(Icons.AutoMirrored.Filled.ReceiptLong, "Receipts", Tone.Success, onClick = withHouse(onNavigateToHouseReceipts)),
                ManageAction(Icons.Default.ReportProblem, "Issues", Tone.Warning, stats.pendingComplaints + stats.overdueComplaints, withHouse(onNavigateToComplaints)),
                ManageAction(Icons.AutoMirrored.Filled.Chat, "Messages", Tone.Info, onClick = onNavigateToMessages),
                ManageAction(Icons.AutoMirrored.Filled.Announcement, "Announce", Tone.Neutral, onClick = withHouse(onNavigateToBroadcast))
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { RevenueHero(stats) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val occupancy = if (stats.totalCapacity > 0) stats.occupiedSpaces * 100 / stats.totalCapacity else 0
                        MetricTile("Occupancy", "$occupancy%", Icons.Default.Bed, Modifier.weight(1f), Tone.Brand)
                        MetricTile("Requests", stats.pendingRequests.toString(), Icons.Default.HourglassTop, Modifier.weight(1f), Tone.Info)
                        MetricTile(
                            "Overdue issues", stats.overdueComplaints.toString(), Icons.Default.Warning, Modifier.weight(1f),
                            if (stats.overdueComplaints > 0) Tone.Danger else Tone.Neutral
                        )
                    }
                }

                item {
                    SectionHeader("Manage")
                    GridRows(items = manageActions, columns = 4, spacing = 10) { action, modifier ->
                        QuickAction(action.icon, action.label, action.onClick, modifier, action.tone, action.badge)
                    }
                }

                item {
                    SectionHeader("Insights")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InsightCard(Icons.Default.Assessment, "Financial report", "Collections & arrears", withHouse(onNavigateToReports), Modifier.weight(1f))
                        InsightCard(Icons.Default.History, "Audit log", "Every change, tracked", withHouse(onNavigateToAuditLogs), Modifier.weight(1f))
                    }
                }

                activeHouse?.let { house ->
                    item {
                        SectionHeader("House details", actionLabel = "Edit", onAction = { onNavigateToEditHouse(house.boardingHouseId) })
                        SurfaceCard {
                            InfoRow("Address", house.address.ifBlank { "—" })
                            InfoRow("Phone", house.phone.ifBlank { "—" })
                            InfoRow("Rooms", stats.totalRooms.toString())
                            InfoRow("Beds", "${stats.occupiedSpaces} of ${stats.totalCapacity} occupied")
                            if (house.description.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(house.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        if (showSwitcher) {
            BoardingHouseSwitcherDialog(
                houses = boardingHouses,
                activeHouseId = activeHouse?.boardingHouseId,
                onDismiss = { showSwitcher = false },
                onSelect = {
                    switcherViewModel.switchBoardingHouse(it)
                    showSwitcher = false
                },
                onAddHouse = {
                    showSwitcher = false
                    onNavigateToAddBoardingHouse()
                }
            )
        }
    }
}

@Composable
private fun RevenueHero(stats: DashboardStats) {
    val total = stats.rentCollected + stats.outstandingRent
    val collectedShare = if (total > 0) (stats.rentCollected / total).toFloat() else 0f
    HeroCard {
        Text("Rent collected", style = MaterialTheme.typography.labelLarge, color = LocalContentColor.current.copy(alpha = 0.8f))
        Spacer(Modifier.height(4.dp))
        Text(formatKwacha(stats.rentCollected), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { collectedShare },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            strokeCap = StrokeCap.Round,
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f)
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Outstanding", style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f))
                Text(formatKwacha(stats.outstandingRent), style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Paid in advance", style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f))
                Text("${stats.advancePaidTenants} tenant(s)", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun InsightCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(onClick = onClick, modifier = modifier) {
        IconBadge(icon, tone = Tone.Info, size = 40)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardingHouseSwitcherDialog(
    houses: List<BoardingHouse>,
    activeHouseId: String?,
    onDismiss: () -> Unit,
    onSelect: (BoardingHouse) -> Unit,
    onAddHouse: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Your boarding houses",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
            items(houses, key = { it.boardingHouseId }) { house ->
                val selected = house.boardingHouseId == activeHouseId
                ListItem(
                    headlineContent = { Text(house.name) },
                    supportingContent = { Text(house.address, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { IconBadge(Icons.Default.Apartment, tone = if (selected) Tone.Brand else Tone.Neutral, size = 40) },
                    trailingContent = { if (selected) Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onSelect(house) }.padding(horizontal = 8.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
        OutlinedButton(
            onClick = onAddHouse,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(52.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.AddHome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add boarding house")
        }
        Spacer(Modifier.navigationBarsPadding())
    }
}
