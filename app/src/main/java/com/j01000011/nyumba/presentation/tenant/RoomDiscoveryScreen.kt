package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Room
import com.j01000011.nyumba.presentation.rooms.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDiscoveryScreen(
    onNavigateBack: () -> Unit,
    onRoomSelected: (String) -> Unit,
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val rooms by roomViewModel.availableRooms.collectAsState()

    LaunchedEffect(Unit) {
        roomViewModel.loadAvailableRooms()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Rooms") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (rooms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No available rooms found at the moment.")
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onRoomSelected(room.roomId) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(room.roomName, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Monthly Rent: K${room.monthlyRent}", style = MaterialTheme.typography.bodyMedium)
                            Text("Spaces Available: ${room.capacity - room.occupiedCount} / ${room.capacity}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(room.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
