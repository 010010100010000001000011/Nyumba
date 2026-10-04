package com.j01000011.nyumba.presentation.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
                            onApprove = { viewModel.approveRequest(request.requestId) },
                            onReject = { viewModel.rejectRequest(request.requestId) }
                        )
                    }
                }
            }
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
            Text("Request Status: ${request.status.name}")
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
                        Text("Approve")
                    }
                }
            }
        }
    }
}
