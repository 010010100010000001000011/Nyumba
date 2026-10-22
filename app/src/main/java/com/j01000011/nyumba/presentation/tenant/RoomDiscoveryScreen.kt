package com.j01000011.nyumba.presentation.tenant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel
import com.j01000011.nyumba.ui.components.EmptyState
import com.j01000011.nyumba.ui.components.NyumbaTopBar

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

    val withSpace = rooms.filter { it.availableCount > 0 }

    Scaffold(topBar = { NyumbaTopBar(title = "Available rooms", onBack = onNavigateBack) }) { padding ->
        if (withSpace.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MeetingRoom,
                title = "No rooms available",
                message = "Every listed room is currently full. Check back soon.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(withSpace, key = { it.roomId }) { room ->
                    AvailableRoomCard(room = room, onClick = { onRoomSelected(room.roomId) })
                }
            }
        }
    }
}
