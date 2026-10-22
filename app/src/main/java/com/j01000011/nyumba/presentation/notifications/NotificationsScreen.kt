package com.j01000011.nyumba.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Notification
import com.j01000011.nyumba.domain.model.NotificationCategory
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.messages.relativeTime
import com.j01000011.nyumba.ui.components.*

@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    var unreadOnly by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let { notificationViewModel.loadNotifications(it.uid) }
    }

    val unreadCount = notifications.count { !it.isRead }
    val visible = notifications
        .filter { !unreadOnly || !it.isRead }
        .sortedByDescending { it.createdAt }

    Scaffold(
        topBar = {
            NyumbaTopBar(
                title = "Notifications",
                subtitle = if (unreadCount > 0) "$unreadCount unread" else "You're up to date",
                onBack = onNavigateBack,
                actions = {
                    if (unreadCount > 0) {
                        TextButton(onClick = { user?.let { notificationViewModel.markAllAsRead(it.uid) } }) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Read all")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !unreadOnly, onClick = { unreadOnly = false }, label = { Text("All") })
                FilterChip(selected = unreadOnly, onClick = { unreadOnly = true }, label = { Text("Unread ($unreadCount)") })
            }
            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.NotificationsNone,
                    title = if (unreadOnly) "All caught up" else "No notifications",
                    message = "Rent reminders, payment confirmations and messages from your landlord or tenants show up here."
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(visible, key = { it.notificationId }) { notification ->
                        NotificationItemCard(
                            notification = notification,
                            onMarkAsRead = { notificationViewModel.markAsRead(notification.notificationId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(notification: Notification, onMarkAsRead: () -> Unit) {
    val (icon, tone) = notification.category.style()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            .clickable(enabled = !notification.isRead, onClick = onMarkAsRead)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        IconBadge(icon, tone = tone, size = 40)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    relativeTime(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(notification.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!notification.isRead) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.padding(top = 6.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
    }
}

private fun NotificationCategory.style(): Pair<ImageVector, Tone> = when (this) {
    NotificationCategory.RENT -> Icons.Default.CalendarMonth to Tone.Warning
    NotificationCategory.PAYMENT -> Icons.Default.Payments to Tone.Success
    NotificationCategory.COMPLAINT -> Icons.Default.ReportProblem to Tone.Danger
    NotificationCategory.ROOM_ASSIGNMENT -> Icons.Default.MeetingRoom to Tone.Brand
    NotificationCategory.MAINTENANCE -> Icons.Default.Build to Tone.Warning
    NotificationCategory.MESSAGE -> Icons.AutoMirrored.Filled.Chat to Tone.Info
    NotificationCategory.SYSTEM -> Icons.Default.Info to Tone.Neutral
    NotificationCategory.GENERAL -> Icons.Default.Campaign to Tone.Info
}
