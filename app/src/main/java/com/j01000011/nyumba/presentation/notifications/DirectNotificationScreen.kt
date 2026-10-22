package com.j01000011.nyumba.presentation.notifications

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Notification
import com.j01000011.nyumba.domain.model.NotificationCategory
import com.j01000011.nyumba.presentation.landlord.InitialsAvatar
import com.j01000011.nyumba.presentation.landlord.TenantManagementViewModel
import com.j01000011.nyumba.ui.components.NyumbaTopBar
import kotlinx.coroutines.launch

@Composable
fun DirectNotificationScreen(
    tenantId: String,
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
    tenantViewModel: TenantManagementViewModel = hiltViewModel()
) {
    val tenant by tenantViewModel.selectedTenant.collectAsState()
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tenantId) {
        tenantViewModel.loadTenantById(tenantId)
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Send notice", onBack = onNavigateBack) }) { padding ->
        NoticeComposer(
            recipientHeader = {
                val name = tenant?.fullName ?: "Tenant"
                RecipientCard(title = name, subtitle = "Appears in their notifications") { InitialsAvatar(name) }
            },
            sendLabel = "Send notice",
            sending = isSending,
            canSend = true,
            modifier = Modifier.padding(padding)
        ) { title, message ->
            isSending = true
            scope.launch {
                viewModel.sendNotification(
                    Notification(
                        tenantId = tenantId,
                        boardingHouseId = tenant?.boardingHouseId,
                        title = title,
                        message = message,
                        category = NotificationCategory.GENERAL
                    )
                )
                isSending = false
                onNavigateBack()
            }
        }
    }
}
