package com.j01000011.nyumba.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j01000011.nyumba.domain.model.Conversation
import com.j01000011.nyumba.domain.model.Message
import com.j01000011.nyumba.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MessageUiState {
    object Idle : MessageUiState()
    object Loading : MessageUiState()
    data class Success(val id: String) : MessageUiState()
    data class Error(val message: String) : MessageUiState()
}

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<MessageUiState>(MessageUiState.Idle)
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    fun loadConversations(userId: String) {
        viewModelScope.launch {
            _uiState.value = MessageUiState.Loading
            messageRepository.getConversations(userId)
                .catch { e ->
                    _uiState.value = MessageUiState.Error(e.message ?: "Failed to load conversations")
                }
                .collectLatest {
                    _conversations.value = it
                    _uiState.value = MessageUiState.Idle
                }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = MessageUiState.Loading
            messageRepository.getMessages(conversationId)
                .catch { e ->
                    _uiState.value = MessageUiState.Error(e.message ?: "Failed to load messages")
                }
                .collectLatest {
                    _messages.value = it
                    _uiState.value = MessageUiState.Idle
                }
        }
    }

    fun sendMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        text: String
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val message = Message(
                conversationId = conversationId,
                senderId = senderId,
                receiverId = receiverId,
                text = text
            )
            messageRepository.sendMessage(message)
                .onFailure {
                    _uiState.value = MessageUiState.Error(it.message ?: "Failed to send message")
                }
        }
    }

    fun getOrCreateConversation(
        tenantId: String,
        landlordId: String,
        boardingHouseId: String,
        tenantName: String,
        landlordName: String
    ) {
        viewModelScope.launch {
            _uiState.value = MessageUiState.Loading
            messageRepository.getOrCreateConversation(tenantId, landlordId, boardingHouseId, tenantName, landlordName)
                .onSuccess { 
                    _uiState.value = MessageUiState.Success(it)
                }
                .onFailure { 
                    _uiState.value = MessageUiState.Error(it.message ?: "Failed to open conversation") 
                }
        }
    }

    fun markAsRead(messageId: String, conversationId: String) {
        viewModelScope.launch {
            messageRepository.markAsRead(messageId, conversationId)
                .onFailure {
                    // Log error or notify user if critical
                }
        }
    }

    fun deleteMessage(messageId: String, conversationId: String, userId: String, isSender: Boolean) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId, conversationId, userId, isSender)
                .onFailure {
                    _uiState.value = MessageUiState.Error(it.message ?: "Failed to delete message")
                }
        }
    }

    fun clearState() {
        _uiState.value = MessageUiState.Idle
    }
}
