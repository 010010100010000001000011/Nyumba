package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Room
import com.j01000011.nyumba.domain.model.RoomStatus
import com.j01000011.nyumba.presentation.messages.MessageUiState
import com.j01000011.nyumba.presentation.messages.MessageViewModel
import com.j01000011.nyumba.presentation.payments.RentViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailsScreen(
    tenantId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToDirectNotice: (String) -> Unit,
    viewModel: TenantManagementViewModel = hiltViewModel(),
    rentViewModel: RentViewModel = hiltViewModel(),
    messageViewModel: MessageViewModel = hiltViewModel(),
    roomViewModel: RoomViewModel = hiltViewModel()
) {
    val tenant by viewModel.selectedTenant.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val rentPeriods by rentViewModel.rentPeriods.collectAsState()
    val messageUiState by messageViewModel.uiState.collectAsState()
    val rooms by roomViewModel.rooms.collectAsState()

    var monthlyRent by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRoomChangePicker by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    LaunchedEffect(tenantId) {
        viewModel.loadTenantById(tenantId)
        rentViewModel.loadTenantRentData(tenantId)
    }

    LaunchedEffect(tenant) {
        tenant?.let {
            monthlyRent = it.monthlyRent.toString()
            dueDay = it.paymentDueDay.toString()
            roomViewModel.loadRooms(it.boardingHouseId ?: "")
        }
    }

    LaunchedEffect(messageUiState) {
        if (messageUiState is MessageUiState.Success) {
            val convId = (messageUiState as MessageUiState.Success).id
            messageViewModel.clearState()
            onNavigateToChat(convId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tenant Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        tenant?.let { t ->
                            messageViewModel.getOrCreateConversation(
                                tenantId = t.uid,
                                landlordId = t.landlordId ?: "",
                                boardingHouseId = t.boardingHouseId ?: "",
                                tenantName = t.fullName,
                                landlordName = "Landlord"
                            )
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message Tenant")
                    }
                    IconButton(onClick = { onNavigateToDirectNotice(tenantId) }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Send Notice")
                    }
                    if (!isEditing) {
                        TextButton(onClick = { isEditing = true }) {
                            Text("Edit Config")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (tenant == null || uiState is TenantUiState.Loading) {
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(tenant!!.fullName, style = MaterialTheme.typography.headlineSmall)
                        Text("Email: ${tenant!!.email}")
                        Text("Phone: ${tenant!!.phoneNumber}")
                        Text("ID: ${tenant!!.identificationNumber}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Emergency Contact: ${tenant!!.emergencyContactName} (${tenant!!.emergencyContactPhone})")
                    }
                }

                if (isEditing) {
                    OutlinedTextField(
                        value = monthlyRent,
                        onValueChange = { monthlyRent = it },
                        label = { Text("Monthly Rent (K)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it },
                        label = { Text("Payment Due Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                        Button(onClick = {
                            viewModel.updateTenantConfig(
                                tenantId,
                                monthlyRent.toDoubleOrNull() ?: 0.0,
                                dueDay.toIntOrNull() ?: 1
                            )
                            isEditing = false
                        }) {
                            Text("Save")
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Rent Configuration", style = MaterialTheme.typography.titleMedium)
                            Text("Monthly Rent: K${tenant!!.monthlyRent}")
                            Text("Due Day: ${tenant!!.paymentDueDay}")
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showRoomChangePicker = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.MeetingRoom, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Room")
                    }
                    Button(
                        onClick = { viewModel.moveOutTenant(tenantId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Move Out")
                    }
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete All Records")
                }

                Text("Rent Ledger", style = MaterialTheme.typography.titleLarge)
                if (rentPeriods.isEmpty()) {
                    Text("No rent periods generated.")
                } else {
                    rentPeriods.forEach { period ->
                        ListItem(
                            headlineContent = { Text("${period.month}/${period.year}") },
                            supportingContent = { Text("Due: K${period.amountDue} | Paid: K${period.amountPaid}") },
                            trailingContent = { Text(period.status.name) }
                        )
                    }
                }
            }
        }

        if (showRoomChangePicker) {
            AlertDialog(
                onDismissRequest = { showRoomChangePicker = false },
                title = { Text("Select New Room") },
                text = {
                    Box(modifier = Modifier.height(300.dp)) {
                        LazyColumn {
                            items(rooms.filter { it.status == RoomStatus.AVAILABLE }) { room ->
                                ListItem(
                                    headlineContent = { Text(room.roomName) },
                                    supportingContent = { Text("Rent: K${room.monthlyRent}") },
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        roomViewModel.changeRoom(tenantId, tenant?.currentRoomId, room.roomId)
                                        showRoomChangePicker = false
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showRoomChangePicker = false }) { Text("Cancel") } }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete All Records?") },
                text = {
                    Column {
                        Text("This is a DESTRUCTIVE action. It will remove all profile, payment, and request data for ${tenant?.fullName}. This cannot be undone.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Type 'DELETE' to confirm:")
                        OutlinedTextField(
                            value = deleteConfirmText,
                            onValueChange = { deleteConfirmText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTenantRecords(tenantId)
                            showDeleteConfirm = false
                            onNavigateBack()
                        },
                        enabled = deleteConfirmText == "DELETE",
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}
