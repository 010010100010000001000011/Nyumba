package com.j01000011.nyumba.presentation.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Message
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    messageViewModel: MessageViewModel = hiltViewModel()
) {
    val user by authViewModel.currentUser.collectAsState()
    val messages by messageViewModel.messages.collectAsState()
    val conversations by messageViewModel.conversations.collectAsState()
    
    val conversation = conversations.find { it.conversationId == conversationId }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    LaunchedEffect(conversationId) {
        messageViewModel.loadMessages(conversationId)
    }

    // Mark unread messages as read
    LaunchedEffect(messages) {
        val uid = user?.uid ?: return@LaunchedEffect
        messages.filter { it.receiverId == uid && it.readAt == null }.forEach {
            messageViewModel.markAsRead(it.messageId, conversationId)
        }
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(conversation?.let { 
                        if (user?.role == com.j01000011.nyumba.domain.model.UserRole.LANDLORD) it.tenantName else it.landlordName 
                    } ?: "Chat") 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                contentPadding = PaddingValues(horizontal = 8.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            val sender = user ?: return@IconButton
                            val currentConv = conversation ?: return@IconButton
                            val receiverId = if (sender.uid == currentConv.landlordId) currentConv.tenantId else currentConv.landlordId
                            messageViewModel.sendMessage(conversationId, sender.uid, receiverId, messageText)
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        val filteredMessages = messages.filter { msg ->
            if (msg.senderId == user?.uid) !msg.deletedForSender else !msg.deletedForReceiver
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(filteredMessages) { message ->
                MessageBubble(
                    message = message,
                    isMine = message.senderId == user?.uid,
                    onLongClick = { messageToDelete = message }
                )
            }
        }

        if (messageToDelete != null) {
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("Delete message?") },
                text = { Text("This will remove the message from your view. The other person will still see it.") },
                confirmButton = {
                    TextButton(onClick = {
                        val u = user ?: return@TextButton
                        val m = messageToDelete ?: return@TextButton
                        messageViewModel.deleteMessage(m.messageId, conversationId, u.uid, m.senderId == u.uid)
                        messageToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messageToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: Message, isMine: Boolean, onLongClick: () -> Unit) {
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(color)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .padding(12.dp)
        ) {
            Text(text = message.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sentAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (isMine && message.readAt != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("✓✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
