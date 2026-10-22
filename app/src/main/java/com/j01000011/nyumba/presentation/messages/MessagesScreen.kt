package com.j01000011.nyumba.presentation.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Conversation
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.landlord.InitialsAvatar
import com.j01000011.nyumba.ui.components.EmptyState
import com.j01000011.nyumba.ui.components.NyumbaTopBar
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    messageViewModel: MessageViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val conversations by messageViewModel.conversations.collectAsState()

    LaunchedEffect(user) {
        user?.let { messageViewModel.loadConversations(it.uid) }
    }

    Scaffold(topBar = { NyumbaTopBar(title = "Messages", onBack = onNavigateBack) }) { padding ->
        if (conversations.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = "No conversations yet",
                message = if (user?.role == UserRole.LANDLORD) "Open a tenant's profile and tap Message to start a conversation."
                else "Your landlord can message you here about your room, rent or repairs.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(conversations, key = { it.conversationId }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        currentUserRole = user?.role ?: UserRole.TENANT,
                        onClick = { onNavigateToChat(conversation.conversationId) }
                    )
                    HorizontalDivider(Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    currentUserRole: UserRole,
    onClick: () -> Unit
) {
    val displayName = (if (currentUserRole == UserRole.LANDLORD) conversation.tenantName else conversation.landlordName).ifBlank { "Conversation" }
    ListItem(
        headlineContent = { Text(displayName, style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            Text(
                conversation.lastMessage.ifBlank { "No messages yet" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = { InitialsAvatar(displayName, size = 48) },
        trailingContent = {
            Text(
                relativeTime(conversation.lastMessageAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp)
    )
}

/** "14:05" today, "Yesterday", weekday within a week, otherwise "3 Oct". */
internal fun relativeTime(millis: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val now = Calendar.getInstance()
    val days = ((startOfDay(now) - startOfDay(then)) / 86_400_000L).toInt()
    return when {
        days <= 0 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
        days == 1 -> "Yesterday"
        days < 7 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}

private fun startOfDay(cal: Calendar): Long = (cal.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis
