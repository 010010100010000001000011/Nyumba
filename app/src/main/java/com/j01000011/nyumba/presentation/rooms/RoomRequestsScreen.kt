package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.RoomRequest
import com.j01000011.nyumba.domain.model.RoomRequestStatus
import com.j01000011.nyumba.presentation.tenant.TenantStatusChip
import com.j01000011.nyumba.presentation.tenant.tone
import com.j01000011.nyumba.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RoomRequestsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val requests by viewModel.roomRequests.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val roomState by viewModel.roomState.collectAsState()
    var selectedRequestForApproval by remember { mutableStateOf<RoomRequest?>(null) }
    var showActiveOnly by remember { mutableStateOf(true) }

    LaunchedEffect(houseId) {
        viewModel.loadRoomRequests(houseId)
        viewModel.loadRooms(houseId)
    }

    val roomNames = remember(rooms) { rooms.associate { it.roomId to it.roomName } }
    val isActive: (RoomRequest) -> Boolean = { it.status == RoomRequestStatus.PENDING || it.status == RoomRequestStatus.HOLDING }
    val visible = requests
        .filter { !showActiveOnly || isActive(it) }
        .sortedWith(compareByDescending<RoomRequest> { isActive(it) }.thenByDescending { it.createdAt })

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Room requests",
                subtitle = "${requests.count(isActive)} awaiting a decision",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = showActiveOnly, onClick = { showActiveOnly = true }, label = { Text("Needs action") })
                FilterChip(selected = !showActiveOnly, onClick = { showActiveOnly = false }, label = { Text("All requests") })
            }
            (roomState as? RoomUiState.Error)?.let {
                InlineMessage(text = it.message, icon = Icons.Default.ErrorOutline, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    title = if (showActiveOnly) "You're all caught up" else "No requests yet",
                    message = "When tenants request or hold a room in this house, they show up here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.requestId }) { request ->
                        RequestItemCard(
                            request = request,
                            roomName = roomNames[request.roomId] ?: "Room",
                            busy = roomState is RoomUiState.Loading,
                            onApprove = { selectedRequestForApproval = request },
                            onReject = { viewModel.rejectRequest(request.requestId) }
                        )
                    }
                }
            }
        }

        selectedRequestForApproval?.let { req ->
            ApproveRequestDialog(
                request = req,
                onDismiss = { selectedRequestForApproval = null },
                onConfirm = { paymentDueDay ->
                    viewModel.approveRequest(req.requestId, paymentDueDay)
                    selectedRequestForApproval = null
                }
            )
        }
    }
}

@Composable
fun RequestItemCard(
    request: RoomRequest,
    roomName: String,
    busy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Default.Person, tone = request.status.tone())
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(request.tenantName.ifBlank { "Tenant" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "$roomName · ${SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(request.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TenantStatusChip(request.status)
        }
        if (request.status == RoomRequestStatus.HOLDING) {
            request.holdUntil?.let {
                val remainingMins = maxOf(0, (it - System.currentTimeMillis()) / 60_000)
                Spacer(Modifier.height(10.dp))
                InlineMessage(
                    text = if (remainingMins > 0) "Bed held for $remainingMins more min" else "Hold has expired",
                    icon = Icons.Default.Timer,
                    tone = Tone.Warning
                )
            }
        }
        if (request.status == RoomRequestStatus.PENDING || request.status == RoomRequestStatus.HOLDING) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Decline")
                }
                Button(onClick = onApprove, enabled = !busy, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Approve")
                }
            }
        }
    }
}

/** Due days are capped at 28 so rent falls on the same date every month, including February. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApproveRequestDialog(
    request: RoomRequest,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dueDay by remember { mutableIntStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Check, contentDescription = null) },
        title = { Text("Approve ${request.tenantName.ifBlank { "tenant" }}?") },
        text = {
            Column {
                Text(
                    "Their rent schedule starts now. Choose the day of the month rent is due.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                        FilterChip(selected = dueDay == day, onClick = { dueDay = day }, label = { Text(ordinal(day)) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Or pick a day", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    FilledTonalIconButton(onClick = { if (dueDay > 1) dueDay-- }) { Text("–") }
                    Text(ordinal(dueDay), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
                    FilledTonalIconButton(onClick = { if (dueDay < 28) dueDay++ }) { Text("+") }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Rent will be due on the ${ordinal(dueDay)} of every month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(dueDay) }) { Text("Approve") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
    return "$day$suffix"
}
