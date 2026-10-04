package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoomScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    var roomName by remember { mutableStateOf("") }
    var capacityString by remember { mutableStateOf("1") }
    var monthlyRentString by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val roomState by viewModel.roomState.collectAsState()

    LaunchedEffect(roomState) {
        if (roomState is RoomUiState.Success) {
            viewModel.clearState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Room") },
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
            OutlinedTextField(
                value = roomName,
                onValueChange = { roomName = it },
                label = { Text("Room Name / Number") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = capacityString,
                onValueChange = { capacityString = it },
                label = { Text("Capacity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = monthlyRentString,
                onValueChange = { monthlyRentString = it },
                label = { Text("Monthly Rent (K)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            if (roomState is RoomUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        val cap = capacityString.toIntOrNull() ?: 1
                        val rent = monthlyRentString.toDoubleOrNull() ?: 0.0
                        viewModel.createRoom(houseId, roomName, cap, rent, description)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Room")
                }
            }

            if (roomState is RoomUiState.Error) {
                Text(
                    text = (roomState as RoomUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
