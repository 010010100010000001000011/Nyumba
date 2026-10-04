package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRoomScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val room by roomViewModel.selectedRoom.collectAsState()
    val house by roomViewModel.selectedHouse.collectAsState()

    LaunchedEffect(user) {
        user?.currentRoomId?.let { roomViewModel.loadRoomById(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Room") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (user?.currentRoomId == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You are not currently assigned to a room.")
                }
            } else {
                room?.let { r ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(r.roomName, style = MaterialTheme.typography.headlineSmall)
                                Text("Monthly Rent: K${r.monthlyRent}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Room Details", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(r.description)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Capacity: ${r.capacity} tenants")
                            Text("Current Occupancy: ${r.occupiedCount}")
                        }
                    }
                    
                    house?.let { h ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(h.name, style = MaterialTheme.typography.titleMedium)
                                    Text(h.address, style = MaterialTheme.typography.bodySmall)
                                    Text("Contact: ${h.phone}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } ?: Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Loading house details...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } ?: Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
