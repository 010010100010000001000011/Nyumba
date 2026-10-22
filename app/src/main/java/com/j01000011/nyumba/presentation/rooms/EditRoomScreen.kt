package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.ui.components.LoadingState
import com.j01000011.nyumba.ui.components.NyumbaTopBar

@Composable
fun EditRoomScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    // Load the room directly; this screen has no house-level room list to look it up in.
    val room by viewModel.selectedRoom.collectAsState()
    val roomState by viewModel.roomState.collectAsState()

    LaunchedEffect(roomId) {
        viewModel.loadRoomById(roomId)
    }
    LaunchedEffect(roomState) {
        if (roomState is RoomUiState.Success) {
            viewModel.clearState()
            onNavigateBack()
        }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Edit room", subtitle = room?.roomName, onBack = onNavigateBack) }) { padding ->
        val r = room?.takeIf { it.roomId == roomId }
        if (r == null) {
            LoadingState(Modifier.padding(padding))
        } else {
            RoomForm(
                initial = remember(r.roomId) {
                    RoomFormValues(
                        name = r.roomName,
                        capacity = r.capacity.toString(),
                        rent = "%.2f".format(java.util.Locale.US, r.monthlyRent),
                        description = r.description,
                        status = r.status
                    )
                },
                submitLabel = "Save changes",
                loading = roomState is RoomUiState.Loading,
                error = (roomState as? RoomUiState.Error)?.message,
                showStatus = true,
                minCapacity = r.occupiedCount,
                modifier = Modifier.padding(padding)
            ) { name, capacity, rent, description, status ->
                viewModel.updateRoom(
                    r.copy(
                        roomName = name,
                        capacity = capacity,
                        monthlyRent = rent,
                        description = description,
                        status = status,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
