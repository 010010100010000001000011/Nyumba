package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.AuditRepository
import com.j01000011.nyumba.domain.repository.TenantRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val auditRepository: AuditRepository
) : BaseRepository(firestore), TenantRepository {

    override fun getTenants(boardingHouseId: String): Flow<List<User>> = callbackFlow {
        val subscription = firestore.collection("users")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .whereEqualTo("role", UserRole.TENANT.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(User::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getTenantById(tenantId: String): Result<User> = try {
        val snapshot = firestore.collection("users").document(tenantId).get().await()
        val tenant = snapshot.toObject(User::class.java) ?: throw Exception("Tenant not found")
        Result.success(tenant)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTenantConfig(
        tenantId: String,
        monthlyRent: Double,
        dueDay: Int
    ): Result<Unit> = try {
        firestore.collection("users").document(tenantId)
            .update(
                "monthlyRent", monthlyRent,
                "paymentDueDay", dueDay
            ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun moveOutTenant(tenantId: String): Result<Unit> = try {
        var houseId: String? = null
        firestore.runTransaction { transaction ->
            val userRef = firestore.collection("users").document(tenantId)
            val user = transaction.get(userRef).toObject(User::class.java) ?: throw Exception("User not found")
            houseId = user.boardingHouseId
            
            val roomId = user.currentRoomId
            if (roomId != null) {
                val roomRef = firestore.collection("rooms").document(roomId)
                val room = transaction.get(roomRef).toObject(Room::class.java) ?: throw Exception("Room not found")
                
                val newOccupiedCount = maxOf(0, room.occupiedCount - 1)
                transaction.update(roomRef, "occupiedCount", newOccupiedCount, "status", RoomStatus.AVAILABLE.name, "updatedAt", System.currentTimeMillis())
            }

            transaction.update(userRef, 
                "isActive", false, 
                "currentRoomId", null,
                "boardingHouseId", null,
                "landlordId", null
            )
            null
        }.await()

        auditRepository.recordLog(AuditLog(
            action = "TENANT_MOVE_OUT",
            entityType = "USER",
            entityId = tenantId,
            boardingHouseId = houseId
        ))

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteTenantRecords(tenantId: String): Result<Unit> = try {
        val collectionsToDeleteFrom = listOf(
            "roomRequests", "payments", "receipts", "rentPeriods", "complaints", "notifications"
        )

        // 1. Delete simple documents
        for (collection in collectionsToDeleteFrom) {
            val snapshot = firestore.collection(collection).whereEqualTo("tenantId", tenantId).get().await()
            firestore.runBatch { batch ->
                snapshot.documents.forEach { batch.delete(it.reference) }
            }.await()
        }

        // 2. Delete messages (Collection Group)
        val senderMessages = firestore.collectionGroup("messages").whereEqualTo("senderId", tenantId).get().await()
        val receiverMessages = firestore.collectionGroup("messages").whereEqualTo("receiverId", tenantId).get().await()
        
        firestore.runBatch { batch ->
            senderMessages.documents.forEach { batch.delete(it.reference) }
            receiverMessages.documents.forEach { batch.delete(it.reference) }
        }.await()

        // Deleted last: security rules authorise the deletes above via the tenant's boardingHouseId
        firestore.collection("users").document(tenantId).delete().await()

        auditRepository.recordLog(AuditLog(
            action = "TENANT_RECORDS_DELETED",
            entityType = "USER",
            entityId = tenantId,
            metadata = mapOf("details" to "All PII, financial, and message records removed")
        ))

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
