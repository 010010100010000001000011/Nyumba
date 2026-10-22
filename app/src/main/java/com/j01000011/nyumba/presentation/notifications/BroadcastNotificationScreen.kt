package com.j01000011.nyumba.presentation.notifications

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Notification
import com.j01000011.nyumba.domain.model.NotificationCategory
import com.j01000011.nyumba.presentation.landlord.TenantManagementViewModel
import com.j01000011.nyumba.ui.components.IconBadge
import com.j01000011.nyumba.ui.components.NyumbaTopBar
import com.j01000011.nyumba.ui.components.Tone
import kotlinx.coroutines.launch

@Composable
fun BroadcastNotificationScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    tenantViewModel: TenantManagementViewModel = hiltViewModel()
) {
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tenants by tenantViewModel.tenants.collectAsState()

    LaunchedEffect(houseId) {
        tenantViewModel.loadTenants(houseId)
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Announcement", onBack = onNavigateBack) }) { padding ->
        NoticeComposer(
            recipientHeader = {
                RecipientCard(
                    title = "All tenants",
                    subtitle = if (tenants.isEmpty()) "No tenants in this house yet" else "${tenants.size} recipient(s) in this house"
                ) { IconBadge(Icons.Default.Groups, tone = Tone.Info) }
            },
            sendLabel = if (tenants.isEmpty()) "Send" else "Send to ${tenants.size} tenant(s)",
            sending = isSending,
            canSend = tenants.isNotEmpty(),
            modifier = Modifier.padding(padding)
        ) { title, message ->
            isSending = true
            scope.launch {
                // One notification per tenant (scoped to the active house); move to a Cloud Function with push later.
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
        }
    }
}
