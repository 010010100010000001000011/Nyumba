package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.rooms.RoomUiState
import com.j01000011.nyumba.presentation.rooms.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailsScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val room by roomViewModel.selectedRoom.collectAsState()
    val roomState by roomViewModel.roomState.collectAsState()

    LaunchedEffect(roomId) {
        roomViewModel.loadRoomById(roomId)
    }

    LaunchedEffect(roomState) {
        if (roomState is RoomUiState.Success) {
            roomViewModel.clearState()
            onNavigateToDashboard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Room Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (roomState is RoomUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (roomState is RoomUiState.Error) {
                Text(
                    text = (roomState as RoomUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                room?.let { r ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(r.roomName, style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Monthly Rent: K${r.monthlyRent}", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Capacity: ${r.capacity} tenants", style = MaterialTheme.typography.bodyLarge)
                            Text("Occupied: ${r.occupiedCount} slots", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Description:", style = MaterialTheme.typography.titleMedium)
                            Text(r.description, style = MaterialTheme.typography.bodyLarge)
                        }

                        Button(
                            onClick = {
                                val u = user ?: return@Button
                                roomViewModel.requestRoom(r.roomId, u.uid, u.fullName, r.boardingHouseId)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = (r.capacity - r.occupiedCount) > 0
                        ) {
                            Text(if ((r.capacity - r.occupiedCount) > 0) "Request Room (Place 30-Min Hold)" else "Room Full")
                        }
                    }
                }
            }
        }
    }
}
