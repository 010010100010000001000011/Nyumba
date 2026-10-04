package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Room
import com.j01000011.nyumba.domain.model.RoomStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoomScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val rooms by viewModel.rooms.collectAsState()
    val room = rooms.find { it.roomId == roomId }
    val roomState by viewModel.roomState.collectAsState()

    var roomName by remember { mutableStateOf("") }
    var capacityString by remember { mutableStateOf("") }
    var monthlyRentString by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(RoomStatus.AVAILABLE) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(room) {
        room?.let {
            roomName = it.roomName
            capacityString = it.capacity.toString()
            monthlyRentString = it.monthlyRent.toString()
            description = it.description
            selectedStatus = it.status
        }
    }

    LaunchedEffect(roomState) {
        if (roomState is RoomUiState.Success) {
            viewModel.clearState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Room") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (room == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
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
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        RoomStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.name) },
                                onClick = {
                                    selectedStatus = status
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (roomState is RoomUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = {
                            val cap = capacityString.toIntOrNull() ?: room.capacity
                            val rent = monthlyRentString.toDoubleOrNull() ?: room.monthlyRent
                            
                            val updatedRoom = room.copy(
                                roomName = roomName,
                                capacity = cap,
                                monthlyRent = rent,
                                description = description,
                                status = selectedStatus,
                                updatedAt = System.currentTimeMillis()
                            )
                            viewModel.updateRoom(updatedRoom)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Changes")
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
}
