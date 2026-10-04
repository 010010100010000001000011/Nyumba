package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.AuditRepository
import com.j01000011.nyumba.domain.repository.MessageRepository
import com.j01000011.nyumba.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val auditRepository: AuditRepository
) : BaseRepository(firestore), MessageRepository {

    override fun getConversations(userId: String): Flow<List<Conversation>> = callbackFlow {
        val subscription = firestore.collection("conversations")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Conversation::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val subscription = firestore.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Message::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun sendMessage(message: Message): Result<String> = try {
        val convRef = firestore.collection("conversations").document(message.conversationId)
        val msgRef = convRef.collection("messages").document()
        val finalMsg = message.copy(messageId = msgRef.id)

        firestore.runBatch { batch ->
            batch.set(msgRef, finalMsg)
            batch.update(convRef, 
                "lastMessage", finalMsg.text,
                "lastMessageAt", finalMsg.sentAt
            )
        }.await()

        // Fetch conversation to determine roles for notification
        val convSnapshot = convRef.get().await()
        val conversation = convSnapshot.toObject(Conversation::class.java)
        
        if (conversation != null) {
            val isLandlordRecipient = message.receiverId == conversation.landlordId
            notificationRepository.sendNotification(
                Notification(
                    tenantId = if (!isLandlordRecipient) message.receiverId else null,
                    landlordId = if (isLandlordRecipient) message.receiverId else null,
                    boardingHouseId = conversation.boardingHouseId,
                    title = "New Message",
                    message = finalMsg.text,
                    category = NotificationCategory.MESSAGE
                )
            )
        }

        Result.success(finalMsg.messageId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getOrCreateConversation(
        tenantId: String,
        landlordId: String,
        boardingHouseId: String,
        tenantName: String,
        landlordName: String
    ): Result<String> = try {
        val query = firestore.collection("conversations")
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("landlordId", landlordId)
            .get().await()

        if (!query.isEmpty) {
            Result.success(query.documents.first().id)
        } else {
            val ref = firestore.collection("conversations").document()
            val conversation = Conversation(
                conversationId = ref.id,
                participants = listOf(tenantId, landlordId),
                tenantId = tenantId,
                landlordId = landlordId,
                boardingHouseId = boardingHouseId,
                tenantName = tenantName,
                landlordName = landlordName
            )
            ref.set(conversation).await()
            
            auditRepository.recordLog(AuditLog(
                action = "CONVERSATION_STARTED",
                entityType = "CONVERSATION",
                entityId = ref.id,
                boardingHouseId = boardingHouseId,
                metadata = mapOf("tenant" to tenantName, "landlord" to landlordName)
            ))
            
            Result.success(ref.id)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAsRead(messageId: String, conversationId: String): Result<Unit> = try {
        firestore.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
            .update("readAt", System.currentTimeMillis()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteMessage(
        messageId: String,
        conversationId: String,
        userId: String,
        isSender: Boolean
    ): Result<Unit> = try {
        val field = if (isSender) "deletedForSender" else "deletedForReceiver"
        firestore.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
            .update(field, true).await()
        
        auditRepository.recordLog(AuditLog(
            action = "MESSAGE_DELETED",
            entityType = "MESSAGE",
            entityId = messageId,
            metadata = mapOf("userId" to userId)
        ))

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
