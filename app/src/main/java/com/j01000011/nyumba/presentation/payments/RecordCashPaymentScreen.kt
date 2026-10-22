package com.j01000011.nyumba.presentation.payments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.landlord.TenantManagementViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel
import com.j01000011.nyumba.ui.components.*

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
    var amountString by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showRoomPicker by remember { mutableStateOf(false) }
    var showTenantPicker by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

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

    val selectedTenant = tenants.find { it.uid == selectedTenantId }
    val selectedRoom = rooms.find { it.roomId == selectedRoomId }
    val amount = amountString.toDoubleOrNull()

    val record = {
        submitted = true
        if (amount != null && amount > 0 && selectedTenantId.isNotEmpty() && selectedRoomId.isNotEmpty()) {
            rentViewModel.recordCashPayment(
                tenantId = selectedTenantId,
                landlordId = user?.uid ?: "",
                boardingHouseId = houseId,
                roomId = selectedRoomId,
                amount = amount,
                notes = notes.ifBlank { null }
            )
        }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Record cash payment", onBack = onNavigateBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader("Who paid?")
            PickerField(
                icon = Icons.Default.Person,
                label = "Tenant",
                value = selectedTenant?.fullName,
                placeholder = "Choose a tenant",
                isError = submitted && selectedTenantId.isEmpty(),
                onClick = { showTenantPicker = true }
            )
            PickerField(
                icon = Icons.Default.MeetingRoom,
                label = "Room",
                value = selectedRoom?.roomName,
                placeholder = "Choose a room",
                isError = submitted && selectedRoomId.isEmpty(),
                onClick = { showRoomPicker = true }
            )

            SectionHeader("Payment")
            if (selectedTenant != null && selectedTenant.monthlyRent > 0) {
                SuggestionChip(
                    onClick = { amountString = "%.2f".format(java.util.Locale.US, selectedTenant.monthlyRent) },
                    label = { Text("Monthly rent · ${formatKwacha(selectedTenant.monthlyRent)}") }
                )
            }
            NyumbaTextField(
                value = amountString,
                onValueChange = { new -> if (new.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) amountString = new },
                label = "Amount received",
                prefix = "K ",
                keyboardType = KeyboardType.Decimal,
                isError = submitted && (amount == null || amount <= 0),
                supportingText = if (submitted && (amount == null || amount <= 0)) "Enter the amount received" else null
            )
            NyumbaTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Reference / note (optional)",
                leadingIcon = Icons.AutoMirrored.Filled.Notes
            )

            (rentState as? RentUiState.Error)?.let {
                InlineMessage(text = it.message, icon = Icons.Default.ErrorOutline)
            }

            PrimaryButton(
                text = if (amount != null && amount > 0) "Record ${formatKwacha(amount)}" else "Record payment",
                onClick = record,
                loading = rentState is RentUiState.Loading,
                icon = Icons.Default.CheckCircle
            )
            Text(
                "The amount is applied to the oldest unpaid month first and a receipt is generated for the tenant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showTenantPicker) {
            ModalBottomSheet(onDismissRequest = { showTenantPicker = false }) {
                Text("Choose tenant", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                if (tenants.isEmpty()) {
                    EmptyState(icon = Icons.Default.Person, title = "No tenants", message = "This house has no active tenants yet.")
                }
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(tenants, key = { it.uid }) { tenant ->
                        ListItem(
                            headlineContent = { Text(tenant.fullName) },
                            supportingContent = { Text(tenant.phoneNumber) },
                            leadingContent = { IconBadge(Icons.Default.Person, tone = Tone.Brand, size = 40) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                selectedTenantId = tenant.uid
                                selectedRoomId = tenant.currentRoomId ?: selectedRoomId
                                showTenantPicker = false
                            }
                        )
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(16.dp))
            }
        }

        if (showRoomPicker) {
            ModalBottomSheet(onDismissRequest = { showRoomPicker = false }) {
                Text("Choose room", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(rooms, key = { it.roomId }) { room ->
                        ListItem(
                            headlineContent = { Text(room.roomName) },
                            supportingContent = { Text("${room.occupiedCount} of ${room.capacity} occupied · ${formatKwacha(room.monthlyRent)}/month") },
                            leadingContent = { IconBadge(Icons.Default.MeetingRoom, tone = Tone.Brand, size = 40) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                selectedRoomId = room.roomId
                                showRoomPicker = false
                            }
                        )
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(16.dp))
            }
        }
    }
}

/** A tappable field that opens a picker, styled like an outlined text field. */
@Composable
internal fun PickerField(
    icon: ImageVector,
    label: String,
    value: String?,
    placeholder: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    OutlinedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = CardDefaults.outlinedCardBorder().let {
            if (isError) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error) else it
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
