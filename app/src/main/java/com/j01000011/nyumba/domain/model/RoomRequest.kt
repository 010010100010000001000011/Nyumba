package com.j01000011.nyumba.domain.model

data class RoomRequest(
    val requestId: String = "",
    val roomId: String = "",
    val tenantId: String = "",
    val tenantName: String = "",
    val boardingHouseId: String = "",
    val status: RoomRequestStatus = RoomRequestStatus.PENDING,
    val holdUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class RoomRequestStatus {
    PENDING,
    HOLDING,
    APPROVED,
    REJECTED,
    EXPIRED
}
