package com.j01000011.nyumba.domain.model

import com.google.firebase.firestore.PropertyName

data class Message(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val sentAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null,
    val deletedForSender: Boolean = false,
    val deletedForReceiver: Boolean = false,
    @get:PropertyName("isDeleted") @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false
)
