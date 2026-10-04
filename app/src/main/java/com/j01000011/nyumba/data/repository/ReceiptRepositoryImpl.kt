package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.j01000011.nyumba.domain.model.Receipt
import com.j01000011.nyumba.domain.repository.ReceiptRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore
) : BaseRepository(firestore), ReceiptRepository {

    override fun getTenantReceipts(tenantId: String): Flow<List<Receipt>> = callbackFlow {
        val subscription = firestore.collection("receipts")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val receipts = snapshot.toObjects(Receipt::class.java)
                    trySend(receipts)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getHouseReceipts(boardingHouseId: String): Flow<List<Receipt>> = callbackFlow {
        val subscription = firestore.collection("receipts")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .orderBy("generatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val receipts = snapshot.toObjects(Receipt::class.java)
                    trySend(receipts)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getReceiptById(receiptId: String): Result<Receipt> {
        return try {
            val snapshot = firestore.collection("receipts").document(receiptId).get().await()
            val receipt = snapshot.toObject(Receipt::class.java) ?: throw Exception("Receipt not found")
            Result.success(receipt)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
