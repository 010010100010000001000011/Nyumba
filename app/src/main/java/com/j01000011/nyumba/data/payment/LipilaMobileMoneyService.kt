package com.j01000011.nyumba.data.payment

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.j01000011.nyumba.domain.model.MobileMoneyPayment
import com.j01000011.nyumba.domain.model.MobileMoneyStatus
import com.j01000011.nyumba.domain.repository.MobileMoneyService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mobile money via Lipila (https://lipila.io). The Lipila API key lives only in Cloud Functions:
 * the app calls `initiateLipilaPayment`, then watches `paymentIntents/{referenceId}`, which the
 * backend updates when Lipila confirms. The payment and receipt are recorded server-side.
 */
@Singleton
class LipilaMobileMoneyService @Inject constructor(
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore
) : MobileMoneyService {

    override suspend fun initiatePayment(phoneNumber: String, amount: Double, notes: String?): Result<MobileMoneyPayment> = try {
        val data = buildMap<String, Any> {
            put("phoneNumber", phoneNumber)
            put("amount", amount)
            if (!notes.isNullOrBlank()) put("notes", notes)
        }
        @Suppress("UNCHECKED_CAST")
        val response = functions.getHttpsCallable("initiateLipilaPayment")
            .call(data).await().getData() as Map<String, Any?>

        Result.success(
            MobileMoneyPayment(
                transactionId = response["referenceId"] as String,
                phoneNumber = phoneNumber,
                amount = amount,
                status = parseStatus(response["status"] as? String),
                message = response["message"] as? String
            )
        )
    } catch (e: FirebaseFunctionsException) {
        // Messages from HttpsError in the function are written for end users
        Result.failure(Exception(e.message ?: "Payment could not be started"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkPaymentStatus(transactionId: String): Result<MobileMoneyPayment> = try {
        val snapshot = firestore.collection(COLLECTION).document(transactionId).get().await()
        Result.success(snapshot.toPayment() ?: throw Exception("Payment not found"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getPaymentUpdates(transactionId: String): Flow<MobileMoneyPayment> = callbackFlow {
        val registration = firestore.collection(COLLECTION).document(transactionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val payment = snapshot?.toPayment() ?: return@addSnapshotListener
                trySend(payment)
                if (payment.status != MobileMoneyStatus.PENDING) close()
            }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toPayment(): MobileMoneyPayment? {
        if (!exists()) return null
        return MobileMoneyPayment(
            transactionId = id,
            phoneNumber = getString("phoneNumber") ?: "",
            amount = getDouble("amount") ?: 0.0,
            status = parseStatus(getString("status")),
            message = getString("message"),
            receiptId = getString("receiptId"),
            timestamp = getLong("updatedAt") ?: System.currentTimeMillis()
        )
    }

    private fun parseStatus(value: String?): MobileMoneyStatus =
        MobileMoneyStatus.entries.firstOrNull { it.name == value } ?: MobileMoneyStatus.PENDING

    private companion object {
        const val COLLECTION = "paymentIntents"
    }
}
