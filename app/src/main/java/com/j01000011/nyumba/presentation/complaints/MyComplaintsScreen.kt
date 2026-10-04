package com.j01000011.nyumba.presentation.complaints

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Complaint
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyComplaintsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubmit: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    complaintViewModel: ComplaintViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val complaints by complaintViewModel.complaints.collectAsState()

    LaunchedEffect(user) {
        user?.let { complaintViewModel.loadTenantComplaints(it.uid) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Complaints") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToSubmit) {
                Icon(Icons.Default.Add, contentDescription = "Submit Complaint")
            }
        }
    ) { padding ->
        if (complaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("You haven't submitted any complaints yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(complaints) { complaint ->
                    ComplaintItemCard(
                        complaint = complaint,
                        onClick = { onNavigateToDetails(complaint.complaintId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ComplaintItemCard(complaint: Complaint, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(complaint.createdAt))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(complaint.title, style = MaterialTheme.typography.titleMedium)
                ComplaintStatusChip(complaint.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Category: ${complaint.category.name}", style = MaterialTheme.typography.bodySmall)
            Text("Submitted: $dateStr", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ComplaintStatusChip(status: com.j01000011.nyumba.domain.model.ComplaintStatus) {
    val color = when (status) {
        com.j01000011.nyumba.domain.model.ComplaintStatus.RESOLVED -> MaterialTheme.colorScheme.primary
        com.j01000011.nyumba.domain.model.ComplaintStatus.PENDING -> MaterialTheme.colorScheme.secondary
        com.j01000011.nyumba.domain.model.ComplaintStatus.OVERDUE -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
