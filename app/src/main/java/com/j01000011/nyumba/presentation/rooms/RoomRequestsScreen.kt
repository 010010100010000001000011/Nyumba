package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.RoomRequest
import com.j01000011.nyumba.domain.model.RoomRequestStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomRequestsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val requests by viewModel.roomRequests.collectAsState()
    val roomState by viewModel.roomState.collectAsState()

    var selectedRequestForApproval by remember { mutableStateOf<RoomRequest?>(null) }

    LaunchedEffect(houseId) {
        viewModel.loadRoomRequests(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Room Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (roomState is RoomUiState.Error) {
                Text(
                    text = (roomState as RoomUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (requests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No room requests found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(requests) { request ->
                        RequestItemCard(
                            request = request,
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
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tenant: ${request.tenantName}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Request Status: ${request.status.name}", style = MaterialTheme.typography.bodyMedium)
            
            if (request.status == RoomRequestStatus.HOLDING) {
                request.holdUntil?.let {
                    val remainingMins = maxOf(0, (it - System.currentTimeMillis()) / 1000 / 60)
                    Text("30-Min Hold active ($remainingMins mins remaining)", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            if (request.status == RoomRequestStatus.PENDING || request.status == RoomRequestStatus.HOLDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Reject")
                    }
                    Button(onClick = onApprove) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveRequestDialog(
    request: RoomRequest,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dueDayText by remember { mutableStateOf("1") }
    val presetDays = listOf(1, 5, 10, 15, 20, 25, 28)

    val dayValue = dueDayText.toIntOrNull()
    val isValidDay = dayValue != null && dayValue in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve Room Request") },
        text = {
            Column {
                Text(
                    text = "Approving tenancy for ${request.tenantName}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Set Monthly Payment Due Day (1 - 31):",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetDays.size) { index ->
                        val day = presetDays[index]
                        FilterChip(
                            selected = dueDayText == day.toString(),
                            onClick = { dueDayText = day.toString() },
                            label = { Text("${day}th") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { input ->
                        if (input.isEmpty() || (input.all { it.isDigit() } && input.length <= 2)) {
                            dueDayText = input
                        }
                    },
                    label = { Text("Custom Payment Due Day") },
                    supportingText = {
                        Text("Rent will be due on the ${dueDayText.ifEmpty { "1" }}st/th of every month.")
                    },
                    isError = !isValidDay,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDay = dayValue?.coerceIn(1, 31) ?: 1
                    onConfirm(finalDay)
                },
                enabled = isValidDay
            ) {
                Text("Confirm & Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
