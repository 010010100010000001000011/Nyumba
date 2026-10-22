package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel
import com.j01000011.nyumba.ui.components.*

@Composable
fun TenantManagementScreen(
    boardingHouseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTenantDetails: (String) -> Unit,
    viewModel: TenantManagementViewModel = hiltViewModel(),
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val tenants by viewModel.tenants.collectAsState()
    val rooms by roomViewModel.rooms.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(boardingHouseId) {
        viewModel.loadTenants(boardingHouseId)
        roomViewModel.loadRooms(boardingHouseId)
    }

    val roomNames = remember(rooms) { rooms.associate { it.roomId to it.roomName } }
    val filtered = tenants
        .filter { it.fullName.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
        .sortedBy { it.fullName.lowercase() }

    Scaffold(
        topBar = { NyumbaTopBar(title = "Tenants", subtitle = "${tenants.size} living here", onBack = onNavigateBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name or phone") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear search") }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.People,
                        title = if (tenants.isEmpty()) "No tenants yet" else "No matches",
                        message = if (tenants.isEmpty()) "Approved room requests add tenants here." else "No tenant matches \"$searchQuery\"."
                    )
                }
            } else {
                items(filtered, key = { it.uid }) { tenant ->
                    SurfaceCard(onClick = { onNavigateToTenantDetails(tenant.uid) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(tenant.fullName)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(tenant.fullName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    listOfNotNull(
                                        tenant.currentRoomId?.let { roomNames[it] ?: "Room" },
                                        tenant.phoneNumber.ifBlank { null }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                formatKwacha(tenant.monthlyRent),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Circle with up to two initials, used wherever a person is listed. */
@Composable
fun InitialsAvatar(name: String, size: Int = 44, tone: Tone = Tone.Brand) {
    val initials = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(tone.containerColor()),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, style = MaterialTheme.typography.titleSmall, color = tone.contentColor())
    }
}
