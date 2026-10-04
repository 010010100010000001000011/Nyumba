package com.j01000011.nyumba.presentation.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Notification
import com.j01000011.nyumba.domain.model.NotificationCategory
import com.j01000011.nyumba.presentation.landlord.TenantManagementViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastNotificationScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    tenantViewModel: TenantManagementViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tenants by tenantViewModel.tenants.collectAsState()

    LaunchedEffect(houseId) {
        tenantViewModel.loadTenants(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broadcast Announcement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Sending to all tenants in this house (${tenants.size} recipients)", style = MaterialTheme.typography.labelLarge)

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Announcement Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Announcement Body") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Button(
                onClick = {
                    isSending = true
                    scope.launch {
                        // In a real broadcast, we'd iterate or use a specialized backend function.
                        // Per instructions: "Broadcast must use active boardingHouseId."
                        // We'll simulate by sending to each tenant in the list.
                        tenants.forEach { tenant ->
                            notificationViewModel.sendNotification(
                                Notification(
                                    tenantId = tenant.uid,
                                    boardingHouseId = houseId,
                                    title = title,
                                    message = message,
                                    category = NotificationCategory.GENERAL
                                )
                            )
                        }
                        isSending = false
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && message.isNotBlank() && !isSending && tenants.isNotEmpty()
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Send Broadcast")
                }
            }
        }
    }
}
