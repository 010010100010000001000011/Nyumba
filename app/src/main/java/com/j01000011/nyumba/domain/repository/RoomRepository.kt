package com.j01000011.nyumba.domain.repository

import com.j01000011.nyumba.domain.model.Room
import com.j01000011.nyumba.domain.model.RoomRequest
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    fun getRooms(boardingHouseId: String): Flow<List<Room>>
    fun getAvailableRooms(): Flow<List<Room>>
    suspend fun createRoom(room: Room): Result<String>
    suspend fun updateRoom(room: Room): Result<Unit>
    suspend fun getRoomById(roomId: String): Result<Room>
    
    // Room Requests & Holds
    fun getRoomRequests(boardingHouseId: String): Flow<List<RoomRequest>>
    fun getTenantRequests(tenantId: String): Flow<List<RoomRequest>>
    suspend fun createRoomRequest(request: RoomRequest): Result<String>
    suspend fun createRoomHold(
        roomId: String, 
        tenantId: String, 
        tenantName: String, 
        boardingHouseId: String, 
        holdDurationMs: Long
    ): Result<String>
    suspend fun updateRoomRequestStatus(requestId: String, status: String): Result<Unit>
    suspend fun approveRoomRequest(requestId: String, paymentDueDay: Int = 1): Result<Unit>
    /** Expires stale holds for one tenant or one boarding house (exactly one must be non-null). */
    suspend fun cleanupExpiredHolds(tenantId: String? = null, boardingHouseId: String? = null): Result<Int>
    suspend fun changeTenantRoom(tenantId: String, oldRoomId: String?, newRoomId: String): Result<Unit>
}
