package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.RoomRepository
import com.j01000011.nyumba.domain.repository.NotificationRepository
import com.j01000011.nyumba.domain.repository.RentRepository
import com.j01000011.nyumba.domain.repository.AuditRepository
import com.j01000011.nyumba.domain.util.RoomCapacity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val rentRepository: RentRepository,
    private val auditRepository: AuditRepository
) : BaseRepository(firestore), RoomRepository {

    override fun getRooms(boardingHouseId: String): Flow<List<Room>> = callbackFlow {
        val subscription = firestore.collection("rooms")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rooms = snapshot.toObjects(Room::class.java)
                    trySend(rooms)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getAvailableRooms(): Flow<List<Room>> = callbackFlow {
        val subscription = firestore.collection("rooms")
            .whereEqualTo("status", RoomStatus.AVAILABLE.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rooms = snapshot.toObjects(Room::class.java)
                    trySend(rooms)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createRoom(room: Room): Result<String> {
        return try {
            val docRef = firestore.collection("rooms").document()
            val newRoom = room.copy(roomId = docRef.id)
            docRef.set(newRoom).await()
            
            auditRepository.recordLog(AuditLog(
                action = "ROOM_CREATED",
                entityType = "ROOM",
                entityId = docRef.id,
                boardingHouseId = room.boardingHouseId,
                metadata = mapOf("name" to room.roomName)
            ))
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRoom(room: Room): Result<Unit> {
        return setDocument("rooms", room.roomId, room)
    }

    override suspend fun getRoomById(roomId: String): Result<Room> {
        return try {
            val snapshot = firestore.collection("rooms").document(roomId).get().await()
            val room = snapshot.toObject(Room::class.java) ?: throw Exception("Room not found")
            Result.success(room)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getRoomRequests(boardingHouseId: String): Flow<List<RoomRequest>> = callbackFlow {
        val subscription = firestore.collection("roomRequests")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.toObjects(RoomRequest::class.java)
                    trySend(requests)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getTenantRequests(tenantId: String): Flow<List<RoomRequest>> = callbackFlow {
        val subscription = firestore.collection("roomRequests")
            .whereEqualTo("tenantId", tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val requests = snapshot.toObjects(RoomRequest::class.java)
                    trySend(requests)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createRoomRequest(request: RoomRequest): Result<String> {
        return try {
            val docRef = firestore.collection("roomRequests").document()
            val newRequest = request.copy(requestId = docRef.id)
            docRef.set(newRequest).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createRoomHold(
        roomId: String,
        tenantId: String,
        tenantName: String,
        boardingHouseId: String,
        holdDurationMs: Long
    ): Result<String> {
        return try {
            val tenant = firestore.collection("users").document(tenantId).get().await().toObject(User::class.java)
            if (tenant?.currentRoomId != null) throw Exception("You already have a room. Move out before requesting another.")
            val active = firestore.collection("roomRequests")
                .whereEqualTo("tenantId", tenantId)
                .whereIn("status", listOf(RoomRequestStatus.PENDING.name, RoomRequestStatus.HOLDING.name))
                .get().await()
            if (!active.isEmpty) throw Exception("You already have a pending room request. Wait for a decision or let it expire.")

            val result = firestore.runTransaction { transaction ->
                val roomRef = firestore.collection("rooms").document(roomId)
                val roomSnapshot = transaction.get(roomRef)
                val room = roomSnapshot.toObject(Room::class.java) ?: throw Exception("Room not found")

                if (room.status != RoomStatus.AVAILABLE || RoomCapacity.freeBeds(room.capacity, room.occupiedCount, room.heldCount) <= 0) {
                    throw Exception("No space available in this room (fully occupied or held)")
                }

                val requestRef = firestore.collection("roomRequests").document()
                val holdUntil = System.currentTimeMillis() + holdDurationMs

                val newRequest = RoomRequest(
                    requestId = requestRef.id,
                    roomId = roomId,
                    tenantId = tenantId,
                    tenantName = tenantName,
                    boardingHouseId = boardingHouseId,
                    status = RoomRequestStatus.HOLDING,
                    holdUntil = holdUntil
                )
                
                transaction.set(requestRef, newRequest)
                // lastRequestId lets the security rules verify this increment matches a new hold
                transaction.update(roomRef, "heldCount", room.heldCount + 1, "updatedAt", System.currentTimeMillis(), "lastRequestId", requestRef.id)
                requestRef.id
            }.await()

            val houseSnapshot = firestore.collection("boardingHouses").document(boardingHouseId).get().await()
            val landlordId = houseSnapshot.getString("landlordId")
            if (landlordId != null) {
                notificationRepository.sendNotification(
                    Notification(
                        landlordId = landlordId,
                        boardingHouseId = boardingHouseId,
                        title = "New Room Request",
                        message = "Tenant $tenantName has requested a room hold.",
                        category = NotificationCategory.ROOM_ASSIGNMENT
                    )
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRoomRequestStatus(requestId: String, status: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection("roomRequests").document(requestId)
                val request = transaction.get(requestRef).toObject(RoomRequest::class.java) ?: throw Exception("Request not found")

                // If moving away from HOLDING, decrement heldCount
                if (request.status == RoomRequestStatus.HOLDING && status != RoomRequestStatus.HOLDING.name) {
                    val roomRef = firestore.collection("rooms").document(request.roomId)
                    val room = transaction.get(roomRef).toObject(Room::class.java) ?: throw Exception("Room not found")
                    transaction.update(roomRef, "heldCount", maxOf(0, room.heldCount - 1), "updatedAt", System.currentTimeMillis())
                }

                transaction.update(requestRef, "status", status, "updatedAt", System.currentTimeMillis())
                null
            }.await()
            
            // Fetch request again for notification (outside transaction for simplicity)
            val requestSnapshot = firestore.collection("roomRequests").document(requestId).get().await()
            val request = requestSnapshot.toObject(RoomRequest::class.java)
            if (request != null) {
                notificationRepository.sendNotification(
                    Notification(
                        tenantId = request.tenantId,
                        boardingHouseId = request.boardingHouseId,
                        title = "Room Request Updated",
                        message = "Your room request status has been changed to $status.",
                        category = NotificationCategory.ROOM_ASSIGNMENT
                    )
                )
                
                auditRepository.recordLog(AuditLog(
                    action = "ROOM_REQUEST_STATUS_UPDATED",
                    entityType = "ROOM_REQUEST",
                    entityId = requestId,
                    boardingHouseId = request.boardingHouseId,
                    metadata = mapOf("status" to status)
                ))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveRoomRequest(requestId: String, paymentDueDay: Int): Result<Unit> {
        return try {
            val roomRequest = firestore.runTransaction { transaction ->
                val requestRef = firestore.collection("roomRequests").document(requestId)
                val requestSnapshot = transaction.get(requestRef)
                val request = requestSnapshot.toObject(RoomRequest::class.java) ?: throw Exception("Request not found")

                if (request.status != RoomRequestStatus.PENDING && request.status != RoomRequestStatus.HOLDING) {
                    throw Exception("Request is no longer pending or holding")
                }

                val roomRef = firestore.collection("rooms").document(request.roomId)
                val roomSnapshot = transaction.get(roomRef)
                val room = roomSnapshot.toObject(Room::class.java) ?: throw Exception("Room not found")

                if (!RoomCapacity.canApprove(room.capacity, room.occupiedCount, room.heldCount, request.status == RoomRequestStatus.HOLDING)) {
                    throw Exception("Room is full (all beds are occupied or held by other tenants)")
                }

                val houseRef = firestore.collection("boardingHouses").document(request.boardingHouseId)
                val landlordId = transaction.get(houseRef).getString("landlordId")
                    ?: throw Exception("Boarding house not found")

                val newOccupiedCount = room.occupiedCount + 1
                val newHeldCount = if (request.status == RoomRequestStatus.HOLDING) maxOf(0, room.heldCount - 1) else room.heldCount
                val newStatus = RoomCapacity.statusAfterChange(room.status, newOccupiedCount, room.capacity)

                transaction.update(roomRef, 
                    "occupiedCount", newOccupiedCount, 
                    "heldCount", newHeldCount,
                    "status", newStatus.name, 
                    "updatedAt", System.currentTimeMillis()
                )
                transaction.update(requestRef, "status", RoomRequestStatus.APPROVED.name, "updatedAt", System.currentTimeMillis())
                
                val userRef = firestore.collection("users").document(request.tenantId)
                transaction.update(userRef, 
                    "currentRoomId", request.roomId, 
                    "boardingHouseId", request.boardingHouseId,
                    "landlordId", landlordId,
                    "monthlyRent", room.monthlyRent,
                    "paymentDueDay", paymentDueDay
                )
                
                request
            }.await()

            val userSnapshot = firestore.collection("users").document(roomRequest.tenantId).get().await()
            val user = userSnapshot.toObject(User::class.java)
            val scheduleResult = user?.let { rentRepository.generateInitialRentPeriods(it) }
                ?: Result.failure(Exception("Tenant profile not found"))

            notificationRepository.sendNotification(
                Notification(
                    tenantId = roomRequest.tenantId,
                    boardingHouseId = roomRequest.boardingHouseId,
                    title = "Room Request Approved",
                    message = "Congratulations! Your room request has been approved.",
                    category = NotificationCategory.ROOM_ASSIGNMENT
                )
            )
            
            auditRepository.recordLog(AuditLog(
                action = "ROOM_REQUEST_APPROVED",
                entityType = "ROOM_REQUEST",
                entityId = requestId,
                boardingHouseId = roomRequest.boardingHouseId,
                metadata = mapOf("tenantId" to roomRequest.tenantId, "roomId" to roomRequest.roomId)
            ))

            scheduleResult.exceptionOrNull()?.let {
                throw Exception("Room approved, but the rent schedule could not be created: ${it.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupExpiredHolds(tenantId: String?, boardingHouseId: String?): Result<Int> {
        return try {
            require((tenantId == null) != (boardingHouseId == null)) { "Specify exactly one of tenantId or boardingHouseId" }
            val now = System.currentTimeMillis()
            val scoped = if (tenantId != null) {
                firestore.collection("roomRequests").whereEqualTo("tenantId", tenantId)
            } else {
                firestore.collection("roomRequests").whereEqualTo("boardingHouseId", boardingHouseId)
            }
            val expiredSnapshots = scoped
                .whereEqualTo("status", RoomRequestStatus.HOLDING.name)
                .whereLessThan("holdUntil", now)
                .get()
                .await()

            var updatedCount = 0
            for (doc in expiredSnapshots.documents) {
                val request = doc.toObject(RoomRequest::class.java) ?: continue
                
                val expired = firestore.runTransaction { transaction ->
                    // Re-check inside the transaction: the hold may have been approved since the query ran
                    val current = transaction.get(doc.reference).toObject(RoomRequest::class.java)
                    if (current?.status != RoomRequestStatus.HOLDING) return@runTransaction false

                    val roomRef = firestore.collection("rooms").document(request.roomId)
                    val room = transaction.get(roomRef).toObject(Room::class.java) ?: throw Exception("Room not found")
                    
                    transaction.update(doc.reference, "status", RoomRequestStatus.EXPIRED.name, "updatedAt", now)
                    // lastRequestId lets the security rules verify this decrement matches an expiring hold
                    if (room.heldCount > 0) {
                        transaction.update(roomRef, "heldCount", room.heldCount - 1, "updatedAt", now, "lastRequestId", doc.id)
                    }
                    true
                }.await()
                if (expired) updatedCount++
            }

            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changeTenantRoom(tenantId: String, oldRoomId: String?, newRoomId: String): Result<Unit> {
        return try {
            if (oldRoomId == newRoomId) throw Exception("The tenant is already in this room")
            val now = System.currentTimeMillis()
            val tenant = firestore.runTransaction { transaction ->
                // All reads first (Firestore transactions require reads before writes)
                val userRef = firestore.collection("users").document(tenantId)
                val user = transaction.get(userRef).toObject(User::class.java) ?: throw Exception("Tenant not found")
                val oldRoomRef = oldRoomId?.let { firestore.collection("rooms").document(it) }
                val oldRoom = oldRoomRef?.let { transaction.get(it).toObject(Room::class.java) }
                val newRoomRef = firestore.collection("rooms").document(newRoomId)
                val newRoom = transaction.get(newRoomRef).toObject(Room::class.java) ?: throw Exception("New room not found")

                if (newRoom.boardingHouseId != user.boardingHouseId) throw Exception("The new room is in a different boarding house")
                if (newRoom.status != RoomStatus.AVAILABLE || RoomCapacity.freeBeds(newRoom.capacity, newRoom.occupiedCount, newRoom.heldCount) <= 0) {
                    throw Exception("The new room has no free bed")
                }

                if (oldRoomRef != null && oldRoom != null) {
                    val oldCount = maxOf(0, oldRoom.occupiedCount - 1)
                    transaction.update(oldRoomRef,
                        "occupiedCount", oldCount,
                        "status", RoomCapacity.statusAfterChange(oldRoom.status, oldCount, oldRoom.capacity).name,
                        "updatedAt", now
                    )
                }
                val newCount = newRoom.occupiedCount + 1
                transaction.update(newRoomRef,
                    "occupiedCount", newCount,
                    "status", RoomCapacity.statusAfterChange(newRoom.status, newCount, newRoom.capacity).name,
                    "updatedAt", now
                )
                transaction.update(userRef, "currentRoomId", newRoomId, "monthlyRent", newRoom.monthlyRent)
                user.copy(currentRoomId = newRoomId, monthlyRent = newRoom.monthlyRent)
            }.await()

            // L8: future months follow the new room's rent
            rentRepository.rescheduleFuturePeriods(tenantId, tenant.boardingHouseId ?: "", tenant.monthlyRent, tenant.paymentDueDay)

            auditRepository.recordLog(AuditLog(
                action = "ROOM_CHANGED",
                entityType = "USER",
                entityId = tenantId,
                metadata = mapOf("oldRoomId" to (oldRoomId ?: "None"), "newRoomId" to newRoomId)
            ))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
