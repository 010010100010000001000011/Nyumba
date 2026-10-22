package com.j01000011.nyumba.presentation.complaints

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.ComplaintStatus
import com.j01000011.nyumba.ui.components.EmptyState
import com.j01000011.nyumba.ui.components.NyumbaTopBar

@Composable
fun HouseComplaintsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    viewModel: ComplaintViewModel = hiltViewModel()
) {
    val complaints by viewModel.complaints.collectAsState()
    var selectedFilter by remember { mutableStateOf<ComplaintStatus?>(null) }

    LaunchedEffect(houseId) {
        viewModel.loadHouseComplaints(houseId)
    }

    val filtered = if (selectedFilter == null) complaints else complaints.filter { it.status == selectedFilter }
    val openCount = complaints.count { it.status != ComplaintStatus.RESOLVED }

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Complaints",
                subtitle = if (openCount == 0) "All caught up" else "$openCount open",
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to "All (${complaints.size})",
                    ComplaintStatus.PENDING to "Pending (${complaints.count { it.status == ComplaintStatus.PENDING }})",
                    ComplaintStatus.OVERDUE to "Overdue (${complaints.count { it.status == ComplaintStatus.OVERDUE }})",
                    ComplaintStatus.RESOLVED to "Resolved (${complaints.count { it.status == ComplaintStatus.RESOLVED }})"
                )
                filters.forEach { (status, label) ->
                    FilterChip(selected = selectedFilter == status, onClick = { selectedFilter = status }, label = { Text(label) })
                }
            }

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.TaskAlt,
                    title = if (selectedFilter == null) "No complaints" else "Nothing here",
                    message = if (selectedFilter == null) "Tenants have not reported any issues for this house." else "No complaints match this filter."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.complaintId }) { complaint ->
                        ComplaintItemCard(complaint = complaint, onClick = { onNavigateToDetails(complaint.complaintId) })
                    }
                }
            }
        }
    }
}
