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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.j01000011.nyumba.domain.model.Message
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.AuthViewModel
import com.j01000011.nyumba.presentation.landlord.InitialsAvatar
import com.j01000011.nyumba.ui.components.EmptyState
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
    // The conversation (needed to know who receives a message) comes from the user's conversation list
    LaunchedEffect(user) {
        user?.let { messageViewModel.loadConversations(it.uid) }
    }

    val visibleMessages = messages.filter { msg ->
        if (msg.senderId == user?.uid) !msg.deletedForSender else !msg.deletedForReceiver
    }

    LaunchedEffect(messages) {
        val uid = user?.uid ?: return@LaunchedEffect
        messages.filter { it.receiverId == uid && it.readAt == null }.forEach {
            messageViewModel.markAsRead(it.messageId, conversationId)
        }
        if (visibleMessages.isNotEmpty()) listState.animateScrollToItem(visibleMessages.size - 1)
    }

    val otherName = conversation?.let {
        (if (user?.role == UserRole.LANDLORD) it.tenantName else it.landlordName).ifBlank { null }
    } ?: "Chat"

    val send = {
        val sender = user
        val conv = conversation
        if (sender != null && conv != null && messageText.isNotBlank()) {
            val receiverId = if (sender.uid == conv.landlordId) conv.tenantId else conv.landlordId
            messageViewModel.sendMessage(conversationId, sender.uid, receiverId, messageText.trim())
            messageText = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(otherName, size = 36)
                        Spacer(Modifier.width(12.dp))
                        Text(otherName, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = send,
                        enabled = messageText.isNotBlank() && conversation != null,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        if (visibleMessages.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "Say hello",
                message = "Messages you send here are delivered instantly and also show up as a notification.",
                modifier = Modifier.padding(padding)
            )
        } else {
            val dayFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(visibleMessages, key = { it.messageId }) { message ->
                    val index = visibleMessages.indexOf(message)
                    val day = dayFormat.format(Date(message.sentAt))
                    if (index == 0 || dayFormat.format(Date(visibleMessages[index - 1].sentAt)) != day) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(50)) {
                                Text(day, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                        }
                    }
                    MessageBubble(
                        message = message,
                        isMine = message.senderId == user?.uid,
                        onLongClick = { messageToDelete = message }
                    )
                }
            }
        }

        messageToDelete?.let { m ->
            AlertDialog(
                onDismissRequest = { messageToDelete = null },
                title = { Text("Delete message?") },
                text = { Text("It's removed from your view only. The other person can still see it.") },
                confirmButton = {
                    TextButton(onClick = {
                        val u = user ?: return@TextButton
                        messageViewModel.deleteMessage(m.messageId, conversationId, u.uid, m.senderId == u.uid)
                        messageToDelete = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { messageToDelete = null }) { Text("Cancel") } }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: Message, isMine: Boolean, onLongClick: () -> Unit) {
    val container = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = if (isMine) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)

    Box(Modifier.fillMaxWidth(), contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(container)
                .combinedClickable(onClick = {}, onLongClick = onLongClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message.text, style = MaterialTheme.typography.bodyLarge, color = content)
            Spacer(Modifier.height(2.dp))
            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sentAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.7f)
                )
                if (isMine) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = if (message.readAt != null) "Read" else "Sent",
                        modifier = Modifier.size(14.dp),
                        tint = if (message.readAt != null) MaterialTheme.colorScheme.inversePrimary else content.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
