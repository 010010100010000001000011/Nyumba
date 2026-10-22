package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.ui.components.NyumbaTopBar

@Composable
fun AddRoomScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val roomState by viewModel.roomState.collectAsState()

    LaunchedEffect(roomState) {
        if (roomState is RoomUiState.Success) {
            viewModel.clearState()
            onNavigateBack()
        }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Add room", onBack = onNavigateBack) }) { padding ->
        RoomForm(
            initial = remember { RoomFormValues() },
            submitLabel = "Save room",
            loading = roomState is RoomUiState.Loading,
            error = (roomState as? RoomUiState.Error)?.message,
            showStatus = false,
            minCapacity = 1,
            modifier = Modifier.padding(padding)
        ) { name, capacity, rent, description, _ ->
            viewModel.createRoom(houseId, name, capacity, rent, description)
        }
    }
}
