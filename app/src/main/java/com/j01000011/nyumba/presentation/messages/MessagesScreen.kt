package com.j01000011.nyumba.presentation.messages

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Conversation
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No conversations yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        currentUserId = user?.uid ?: "",
                        currentUserRole = user?.role ?: UserRole.TENANT,
                        onClick = { onNavigateToChat(conversation.conversationId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    currentUserRole: UserRole,
    onClick: () -> Unit
) {
    val displayName = if (currentUserRole == UserRole.LANDLORD) conversation.tenantName else conversation.landlordName
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(conversation.lastMessageAt))

    ListItem(
        headlineContent = { Text(displayName, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(conversation.lastMessage, maxLines = 1) },
        trailingContent = { Text(dateStr, style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier.clickable { onClick() }
    )
}
