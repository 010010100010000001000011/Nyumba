package com.j01000011.nyumba.presentation.landlord

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantManagementScreen(
    boardingHouseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToTenantDetails: (String) -> Unit,
    viewModel: TenantManagementViewModel = hiltViewModel()
) {
    val tenants by viewModel.tenants.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(boardingHouseId) {
        viewModel.loadTenants(boardingHouseId)
    }

    val filteredTenants = tenants.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) || 
        it.phoneNumber.contains(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tenants") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search tenants...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (filteredTenants.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tenants found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTenants) { tenant ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToTenantDetails(tenant.uid) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(tenant.fullName, style = MaterialTheme.typography.titleMedium)
                                Text("Phone: ${tenant.phoneNumber}", style = MaterialTheme.typography.bodySmall)
                                Text("Room: ${tenant.currentRoomId ?: "Unassigned"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
