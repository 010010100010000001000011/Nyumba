package com.j01000011.nyumba.domain.model

data class Room(
    val roomId: String = "",
    val boardingHouseId: String = "",
    val roomName: String = "",
    val capacity: Int = 1,
    val occupiedCount: Int = 0,
    val heldCount: Int = 0, // Track active temporary holds atomically
    val monthlyRent: Double = 0.0,
    val description: String = "",
    val status: RoomStatus = RoomStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val availableCount: Int get() = capacity - occupiedCount - heldCount
}

enum class RoomStatus {
    AVAILABLE,
    FULL,
    MAINTENANCE,
    INACTIVE
}
