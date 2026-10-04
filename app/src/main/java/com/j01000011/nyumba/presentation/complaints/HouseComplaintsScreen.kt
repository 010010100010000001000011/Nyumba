package com.j01000011.nyumba.presentation.complaints

import androidx.compose.foundation.clickable
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
import com.j01000011.nyumba.domain.model.ComplaintStatus

@OptIn(ExperimentalMaterial3Api::class)
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

    val filteredComplaints = if (selectedFilter == null) {
        complaints
    } else {
        complaints.filter { it.status == selectedFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("House Complaints") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Simple Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedFilter == ComplaintStatus.PENDING,
                    onClick = { selectedFilter = ComplaintStatus.PENDING },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = selectedFilter == ComplaintStatus.RESOLVED,
                    onClick = { selectedFilter = ComplaintStatus.RESOLVED },
                    label = { Text("Resolved") }
                )
            }

            if (filteredComplaints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No complaints found matching this filter.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredComplaints) { complaint ->
                        ComplaintItemCard(
                            complaint = complaint,
                            onClick = { onNavigateToDetails(complaint.complaintId) }
                        )
                    }
                }
            }
        }
    }
}
