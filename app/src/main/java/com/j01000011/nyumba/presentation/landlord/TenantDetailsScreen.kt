package com.j01000011.nyumba.presentation.landlord

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.RoomStatus
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.messages.MessageUiState
import com.j01000011.nyumba.presentation.messages.MessageViewModel
import com.j01000011.nyumba.presentation.payments.RentStatusChip
import com.j01000011.nyumba.presentation.payments.RentViewModel
import com.j01000011.nyumba.presentation.rooms.RoomViewModel
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

private enum class PendingAction { None, UpdateConfig, MoveOut, Delete }

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
    roomViewModel: RoomViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val tenant by viewModel.selectedTenant.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val rentPeriods by rentViewModel.rentPeriods.collectAsState()
    val messageUiState by messageViewModel.uiState.collectAsState()
    val rooms by roomViewModel.rooms.collectAsState()
    val landlord by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var monthlyRent by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMoveOutConfirm by remember { mutableStateOf(false) }
    var showRoomChangePicker by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf(PendingAction.None) }

    LaunchedEffect(tenantId) {
        viewModel.loadTenantById(tenantId)
        rentViewModel.loadTenantRentData(tenantId)
    }

    LaunchedEffect(tenant) {
        tenant?.let {
            monthlyRent = "%.2f".format(Locale.US, it.monthlyRent)
            dueDay = it.paymentDueDay.toString()
            roomViewModel.loadRooms(it.boardingHouseId ?: "")
        }
    }

    // Leave the screen only after a destructive action has finished, so it is not cancelled midway.
    LaunchedEffect(uiState) {
        if (uiState is TenantUiState.Success) {
            val finished = pending
            pending = PendingAction.None
            viewModel.clearState()
            when (finished) {
                PendingAction.MoveOut, PendingAction.Delete -> onNavigateBack()
                PendingAction.UpdateConfig -> viewModel.loadTenantById(tenantId)
                PendingAction.None -> Unit
            }
        }
    }

    LaunchedEffect(messageUiState) {
        if (messageUiState is MessageUiState.Success) {
            val convId = (messageUiState as MessageUiState.Success).id
            messageViewModel.clearState()
            onNavigateToChat(convId)
        }
    }

    val currentRoom = rooms.find { it.roomId == tenant?.currentRoomId }
    val busy = uiState is TenantUiState.Loading && pending != PendingAction.None

    Scaffold(topBar = { NyumbaTopBar(title = "Tenant", onBack = onNavigateBack) }) { padding ->
        val t = tenant
        if (t == null || t.uid != tenantId) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(t.fullName, size = 56)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.fullName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            currentRoom?.roomName ?: "No room assigned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContactAction(Icons.AutoMirrored.Filled.Chat, "Message", Modifier.weight(1f)) {
                        val l = landlord ?: return@ContactAction
                        messageViewModel.getOrCreateConversation(
                            tenantId = t.uid,
                            landlordId = l.uid,
                            boardingHouseId = t.boardingHouseId ?: "",
                            tenantName = t.fullName,
                            landlordName = l.fullName.ifBlank { "Landlord" }
                        )
                    }
                    ContactAction(Icons.Default.Call, "Call", Modifier.weight(1f), enabled = t.phoneNumber.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${t.phoneNumber}")))
                    }
                    ContactAction(Icons.Default.Campaign, "Notice", Modifier.weight(1f)) { onNavigateToDirectNotice(tenantId) }
                }
            }

            // Contact & ID
            SurfaceCard {
                Text("Details", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                InfoRow("Phone", t.phoneNumber.ifBlank { "—" })
                InfoRow("Email", t.email.ifBlank { "—" })
                InfoRow("NRC / ID", t.identificationNumber.ifBlank { "—" })
                InfoRow(
                    "Emergency contact",
                    listOf(t.emergencyContactName, t.emergencyContactPhone).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "—" }
                )
                InfoRow("Member since", SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(t.createdAt)))
            }

            // Rent configuration
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rent", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (!isEditing) TextButton(onClick = { isEditing = true }) { Text("Edit") }
                }
                if (isEditing) {
                    Spacer(Modifier.height(8.dp))
                    NyumbaTextField(
                        value = monthlyRent,
                        onValueChange = { if (it.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) monthlyRent = it },
                        label = "Monthly rent", prefix = "K ", keyboardType = KeyboardType.Decimal
                    )
                    Spacer(Modifier.height(10.dp))
                    val dueDayValue = dueDay.toIntOrNull()
                    NyumbaTextField(
                        value = dueDay,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dueDay = it },
                        label = "Due day of month", keyboardType = KeyboardType.Number,
                        isError = dueDayValue == null || dueDayValue !in 1..28,
                        supportingText = "Between 1 and 28"
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { isEditing = false }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) { Text("Cancel") }
                        Button(
                            onClick = {
                                pending = PendingAction.UpdateConfig
                                viewModel.updateTenantConfig(tenantId, monthlyRent.toDoubleOrNull() ?: t.monthlyRent, dueDayValue ?: t.paymentDueDay)
                                isEditing = false
                            },
                            enabled = dueDayValue != null && dueDayValue in 1..28 && monthlyRent.toDoubleOrNull() != null,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Save") }
                    }
                } else {
                    InfoRow("Monthly rent", formatKwacha(t.monthlyRent))
                    InfoRow("Due day", "Day ${t.paymentDueDay} of each month")
                    val outstanding = rentPeriods.filter { it.isCurrentOrPastDue }.sumOf { it.remainingBalance }
                    InfoRow("Outstanding now", formatKwacha(outstanding))
                }
            }

            // Ledger
            SectionHeader("Rent ledger")
            if (rentPeriods.isEmpty()) {
                Text("No rent periods yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                SurfaceCard {
                    val monthFmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    rentPeriods.forEachIndexed { index, period ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                val cal = Calendar.getInstance().apply { set(period.year, period.month - 1, 1) }
                                Text(monthFmt.format(cal.time), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${formatKwacha(period.amountPaid)} of ${formatKwacha(period.amountDue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RentStatusChip(period.status)
                        }
                    }
                }
            }

            // Tenancy actions
            SectionHeader("Manage tenancy")
            OutlinedButton(
                onClick = { showRoomChangePicker = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Move to another room")
            }
            OutlinedButton(
                onClick = { showMoveOutConfirm = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy && pending == PendingAction.MoveOut) "Moving out..." else "Move out")
            }
            TextButton(
                onClick = { showDeleteConfirm = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy && pending == PendingAction.Delete) "Deleting..." else "Delete all tenant records")
            }
            (uiState as? TenantUiState.Error)?.let { InlineMessage(it.message, Icons.Default.ErrorOutline) }
            Spacer(Modifier.height(8.dp))
        }

        if (showRoomChangePicker) {
            ModalBottomSheet(onDismissRequest = { showRoomChangePicker = false }) {
                Text(
                    "Move ${t.fullName.substringBefore(' ')} to",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                val options = rooms.filter { it.status == RoomStatus.AVAILABLE && it.availableCount > 0 && it.roomId != t.currentRoomId }
                if (options.isEmpty()) {
                    EmptyState(icon = Icons.Default.MeetingRoom, title = "No free rooms", message = "Every other room in this house is full.")
                }
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(options, key = { it.roomId }) { room ->
                        ListItem(
                            headlineContent = { Text(room.roomName) },
                            supportingContent = { Text("${room.availableCount} free bed(s) · ${formatKwacha(room.monthlyRent)}/month") },
                            leadingContent = { IconBadge(Icons.Default.MeetingRoom, tone = Tone.Brand, size = 40) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                roomViewModel.changeRoom(tenantId, t.currentRoomId, room.roomId)
                                showRoomChangePicker = false
                            }
                        )
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(16.dp))
            }
        }

        if (showMoveOutConfirm) {
            AlertDialog(
                onDismissRequest = { showMoveOutConfirm = false },
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                title = { Text("Move out ${t.fullName}?") },
                text = { Text("Their bed is freed and they are removed from this house. Payment history and receipts are kept.") },
                confirmButton = {
                    Button(onClick = {
                        showMoveOutConfirm = false
                        pending = PendingAction.MoveOut
                        viewModel.moveOutTenant(tenantId)
                    }) { Text("Move out") }
                },
                dismissButton = { TextButton(onClick = { showMoveOutConfirm = false }) { Text("Cancel") } }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Delete all records?") },
                text = {
                    Column {
                        Text("This permanently removes ${t.fullName}'s profile, payments, receipts, rent schedule, complaints and messages. It cannot be undone.")
                        Spacer(Modifier.height(16.dp))
                        NyumbaTextField(value = deleteConfirmText, onValueChange = { deleteConfirmText = it }, label = "Type DELETE to confirm")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            pending = PendingAction.Delete
                            viewModel.deleteTenantRecords(tenantId)
                        },
                        enabled = deleteConfirmText == "DELETE",
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun ContactAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}
