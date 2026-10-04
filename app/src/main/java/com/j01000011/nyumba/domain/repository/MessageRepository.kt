package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.Conversation
import com.j01000011.nyumba.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getConversations(userId: String): Flow<List<Conversation>>
    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<String>
    suspend fun getOrCreateConversation(tenantId: String, landlordId: String, boardingHouseId: String, tenantName: String, landlordName: String): Result<String>
    suspend fun markAsRead(messageId: String, conversationId: String): Result<Unit>
    suspend fun deleteMessage(messageId: String, conversationId: String, userId: String, isSender: Boolean): Result<Unit>
}
