package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomManagementScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddRoom: (String) -> Unit,
    onNavigateToEditRoom: (String) -> Unit,
    onNavigateToRoomRequests: (String) -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val rooms by viewModel.rooms.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadRooms(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Rooms") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onNavigateToRoomRequests(houseId) }) {
                        Text("Requests")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddRoom(houseId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Room")
            }
        }
    ) { padding ->
        if (rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No rooms created yet for this boarding house.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(rooms) { room ->
                    RoomItemCard(
                        room = room,
                        onEditClick = { onNavigateToEditRoom(room.roomId) },
                        onCapacityChange = { newCap -> viewModel.updateRoomCapacity(room, newCap) }
                    )
                }
            }
        }
    }
}

@Composable
fun RoomItemCard(room: Room, onEditClick: () -> Unit, onCapacityChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(room.roomName, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Room", tint = MaterialTheme.colorScheme.primary)
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(room.status.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Monthly Rent: K${room.monthlyRent}")
            Text("Description: ${room.description}")
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Occupied spaces: ${room.occupiedCount} / ${room.capacity}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onCapacityChange(room.capacity - 1) }) {
                        Text("[-]")
                    }
                    Text("${room.capacity}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
                    TextButton(onClick = { onCapacityChange(room.capacity + 1) }) {
                        Text("[+]")
                    }
                }
            }
        }
    }
}
