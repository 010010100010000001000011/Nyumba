package com.j01000011.nyumba.domain.model

data class Conversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Long = System.currentTimeMillis(),
    val tenantId: String = "",
    val landlordId: String = "",
    val boardingHouseId: String = "",
    val tenantName: String = "",
    val landlordName: String = ""
)
