package com.j01000011.nyumba.presentation.complaints

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.ui.components.EmptyState
import com.j01000011.nyumba.ui.components.NyumbaTopBar

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
        topBar = { NyumbaTopBar(title = "My complaints", subtitle = "Report and track issues", onBack = onNavigateBack) },
        floatingActionButton = {
            if (complaints.isNotEmpty()) ExtendedFloatingActionButton(
                onClick = onNavigateToSubmit,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Report issue") }
            )
        }
    ) { padding ->
        if (complaints.isEmpty()) {
            EmptyState(
                icon = Icons.Default.SupportAgent,
                title = "No complaints yet",
                message = "Something broken or not right? Report it and your landlord is notified straight away.",
                actionLabel = "Report an issue",
                onAction = onNavigateToSubmit,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(complaints, key = { it.complaintId }) { complaint ->
                    ComplaintItemCard(complaint = complaint, onClick = { onNavigateToDetails(complaint.complaintId) })
                }
            }
        }
    }
}
