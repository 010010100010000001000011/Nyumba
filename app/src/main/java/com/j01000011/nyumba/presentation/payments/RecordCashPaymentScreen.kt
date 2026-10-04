package com.j01000011.nyumba.presentation.payments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.landlord.TenantManagementViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCashPaymentScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    roomViewModel: RoomViewModel = hiltViewModel(),
    tenantViewModel: TenantManagementViewModel = hiltViewModel(),
    rentViewModel: RentViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val rooms by roomViewModel.rooms.collectAsState()
    val tenants by tenantViewModel.tenants.collectAsState()
    val rentState by rentViewModel.rentState.collectAsState()

    var selectedRoomId by remember { mutableStateOf("") }
    var selectedTenantId by remember { mutableStateOf("") }
    var selectedTenantName by remember { mutableStateOf("") }
    var amountString by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var showRoomPicker by remember { mutableStateOf(false) }
    var showTenantPicker by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) {
        roomViewModel.loadRooms(houseId)
        tenantViewModel.loadTenants(houseId)
    }

    LaunchedEffect(rentState) {
        if (rentState is RentUiState.Success) {
            rentViewModel.clearState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Cash Payment") },
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
            Text("Recipient Information", style = MaterialTheme.typography.titleMedium)
            
            OutlinedButton(
                onClick = { showTenantPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedTenantId.isEmpty()) "Select Tenant" else "Tenant: $selectedTenantName")
            }

            OutlinedButton(
                onClick = { showRoomPicker = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedTenantId.isNotEmpty()
            ) {
                Text(if (selectedRoomId.isEmpty()) "Select Room" else "Room: $selectedRoomId")
            }

            HorizontalDivider()

            Text("Payment Details", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = amountString,
                onValueChange = { amountString = it },
                label = { Text("Amount Paid (K)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Reference / Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            if (rentState is RentUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        val amount = amountString.toDoubleOrNull() ?: 0.0
                        val landlordId = user?.uid ?: ""
                        
                        if (amount > 0 && selectedTenantId.isNotEmpty() && selectedRoomId.isNotEmpty()) {
                            rentViewModel.recordCashPayment(
                                tenantId = selectedTenantId, 
                                landlordId = landlordId,
                                boardingHouseId = houseId,
                                roomId = selectedRoomId,
                                amount = amount,
                                notes = notes
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amountString.isNotBlank() && selectedTenantId.isNotEmpty() && selectedRoomId.isNotEmpty()
                ) {
                    Text("Confirm Cash Payment")
                }
            }

            if (rentState is RentUiState.Error) {
                Text(text = (rentState as RentUiState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        }

        if (showTenantPicker) {
            AlertDialog(
                onDismissRequest = { showTenantPicker = false },
                title = { Text("Select Tenant") },
                text = {
                    Box(modifier = Modifier.height(300.dp)) {
                        LazyColumn {
                            items(tenants) { tenant ->
                                ListItem(
                                    headlineContent = { Text(tenant.fullName) },
                                    supportingContent = { Text("Phone: ${tenant.phoneNumber}") },
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedTenantId = tenant.uid
                                        selectedTenantName = tenant.fullName
                                        selectedRoomId = tenant.currentRoomId ?: ""
                                        showTenantPicker = false
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showTenantPicker = false }) { Text("Cancel") } }
            )
        }

        if (showRoomPicker) {
            AlertDialog(
                onDismissRequest = { showRoomPicker = false },
                title = { Text("Select Room") },
                text = {
                    Box(modifier = Modifier.height(300.dp)) {
                        LazyColumn {
                            items(rooms) { room ->
                                ListItem(
                                    headlineContent = { Text(room.roomName) },
                                    supportingContent = { Text("Occupied slots: ${room.occupiedCount}") },
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedRoomId = room.roomId
                                        showRoomPicker = false
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showRoomPicker = false }) { Text("Cancel") } }
            )
        }
    }
}
