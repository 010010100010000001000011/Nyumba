package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseRoomsDiscoveryScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onRoomSelected: (String) -> Unit,
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val rooms by roomViewModel.rooms.collectAsState()
    val house by roomViewModel.selectedHouse.collectAsState()

    LaunchedEffect(houseId) {
        roomViewModel.loadRooms(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(house?.name ?: "Boarding House Rooms") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                house?.let { h ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = h.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (h.address.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(h.address, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (h.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(h.phone, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (h.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(h.description, style = MaterialTheme.typography.bodySmall)
                            }
                            if (h.houseRules.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("House Rules: ${h.houseRules}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Rooms in this Boarding House",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val availableRooms = rooms.filter { (it.capacity - it.occupiedCount) > 0 }

            if (availableRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No available rooms in this boarding house at the moment.")
                        }
                    }
                }
            } else {
                items(availableRooms) { room ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onRoomSelected(room.roomId) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(room.roomName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "K${room.monthlyRent} / mo",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Spaces Available: ${room.capacity - room.occupiedCount} / ${room.capacity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (room.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(room.description, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onRoomSelected(room.roomId) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Select Room & Request")
                            }
                        }
                    }
                }
            }
        }
    }
}
