package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.j01000011.nyumba.domain.model.AuditLog
import com.j01000011.nyumba.domain.repository.AuditRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore
) : BaseRepository(firestore), AuditRepository {

    override fun getAuditLogs(boardingHouseId: String): Flow<List<AuditLog>> = callbackFlow {
        val subscription = firestore.collection("auditLogs")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(AuditLog::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun recordLog(log: AuditLog): Result<String> = try {
        val ref = firestore.collection("auditLogs").document()
        val finalLog = log.copy(logId = ref.id)
        ref.set(finalLog).await()
        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
