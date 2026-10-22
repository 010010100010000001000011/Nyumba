package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.AuditRepository
import com.j01000011.nyumba.domain.repository.NotificationRepository
import com.j01000011.nyumba.domain.repository.RentRepository
import com.j01000011.nyumba.domain.util.RentAllocationEngine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RentRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val auditRepository: AuditRepository
) : BaseRepository(firestore), RentRepository {

    companion object {
        const val STAGE_7_DAY = "7_DAY"
        const val STAGE_3_DAY = "3_DAY"
        const val STAGE_DUE_DAY = "DUE_DAY"
        const val STAGE_OVERDUE_1 = "OVERDUE_1"
        const val STAGE_OVERDUE_3 = "OVERDUE_3"
        const val STAGE_OVERDUE_7 = "OVERDUE_7"
    }

    override fun getRentPeriods(tenantId: String): Flow<List<RentPeriod>> = callbackFlow {
        val subscription = firestore.collection("rentPeriods")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("year", Query.Direction.ASCENDING)
            .orderBy("month", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val periods = snapshot.toObjects(RentPeriod::class.java)
                    trySend(periods)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getHouseRentPeriods(boardingHouseId: String): Flow<List<RentPeriod>> = callbackFlow {
        val subscription = firestore.collection("rentPeriods")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val periods = snapshot.toObjects(RentPeriod::class.java)
                    trySend(periods)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getPaymentHistory(tenantId: String): Flow<List<Payment>> = callbackFlow {
        val subscription = firestore.collection("payments")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("paymentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val payments = snapshot.toObjects(Payment::class.java)
                    trySend(payments)
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getLandlordPayments(boardingHouseId: String): Flow<List<Payment>> = callbackFlow {
        val subscription = firestore.collection("payments")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .orderBy("paymentDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val payments = snapshot.toObjects(Payment::class.java)
                    trySend(payments)
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun recordPayment(payment: Payment): Result<Unit> {
        return try {
            processPaymentAllocation(payment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processPaymentAllocation(payment: Payment) {
        val paymentRef = firestore.collection("payments").document()
        val finalPayment = payment.copy(paymentId = paymentRef.id, status = PaymentStatus.SUCCESSFUL)
        
        firestore.collection("payments").document(paymentRef.id).set(finalPayment).await()
        
        val unpaidPeriods = firestore.collection("rentPeriods")
            .whereEqualTo("tenantId", payment.tenantId)
            .whereNotEqualTo("status", RentStatus.PAID.name)
            .get().await().toObjects(RentPeriod::class.java)
            .sortedWith(compareBy({ it.year }, { it.month }))

        var remainingBalanceAfterAllocation = 0.0
        var advancePeriodsCoveredCount = 0
        
        if (unpaidPeriods.isNotEmpty()) {
            val updatedPeriods = RentAllocationEngine.allocatePayment(payment.amount, unpaidPeriods)
            
            firestore.runBatch { batch ->
                for (period in updatedPeriods) {
                    batch.set(firestore.collection("rentPeriods").document(period.periodId), period)
                    if (period.status == RentStatus.ADVANCE_PAID) {
                        advancePeriodsCoveredCount++
                    }
                }
            }.await()
            
            remainingBalanceAfterAllocation = updatedPeriods.sumOf { maxOf(0.0, it.amountDue - it.amountPaid) }
        }

        // Fetch Metadata for Receipt
        val tenantSnapshot = firestore.collection("users").document(payment.tenantId).get().await()
        val tenantName = tenantSnapshot.getString("fullName") ?: "Tenant"
        
        val roomSnapshot = firestore.collection("rooms").document(payment.roomId).get().await()
        val roomName = roomSnapshot.getString("roomName") ?: "Room"

        val receiptRef = firestore.collection("receipts").document()
        val receipt = Receipt(
            receiptId = receiptRef.id,
            receiptNumber = "REC-${System.currentTimeMillis() / 1000}",
            paymentId = paymentRef.id,
            tenantId = payment.tenantId,
            landlordId = payment.landlordId,
            boardingHouseId = payment.boardingHouseId,
            roomId = payment.roomId,
            tenantName = tenantName,
            roomName = roomName,
            amountPaid = payment.amount,
            remainingBalance = remainingBalanceAfterAllocation,
            advanceCoverage = advancePeriodsCoveredCount,
            paymentMethod = payment.paymentMethod,
            transactionId = payment.transactionId,
            paymentDate = payment.paymentDate,
            generatedAt = System.currentTimeMillis()
        )
        firestore.collection("receipts").document(receipt.receiptId).set(receipt).await()

        notificationRepository.sendNotification(
            Notification(
                tenantId = payment.tenantId,
                boardingHouseId = payment.boardingHouseId,
                title = "Payment Successful",
                message = "Your payment of K${payment.amount} was recorded. Receipt generated.",
                category = NotificationCategory.PAYMENT
            )
        )

        auditRepository.recordLog(AuditLog(
            action = "PAYMENT_RECORDED",
            entityType = "PAYMENT",
            entityId = paymentRef.id,
            boardingHouseId = payment.boardingHouseId,
            metadata = mapOf("amount" to payment.amount.toString(), "method" to payment.paymentMethod.name)
        ))
    }

    override suspend fun generateInitialRentPeriods(tenant: User): Result<Unit> {
        return try {
            val calendar = Calendar.getInstance()
            firestore.runBatch { batch ->
                for (i in 0 until 12) {
                    val currentCal = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, i) }
                    val ref = firestore.collection("rentPeriods").document()
                    val period = RentPeriod(
                        periodId = ref.id,
                        tenantId = tenant.uid,
                        boardingHouseId = tenant.boardingHouseId ?: "",
                        roomId = tenant.currentRoomId ?: "",
                        month = currentCal.get(Calendar.MONTH) + 1,
                        year = currentCal.get(Calendar.YEAR),
                        amountDue = tenant.monthlyRent,
                        status = RentStatus.UNPAID,
                        dueDate = currentCal.apply { set(Calendar.DAY_OF_MONTH, tenant.paymentDueDay) }.timeInMillis
                    )
                    batch.set(ref, period)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkAndSendRentReminders(tenantId: String): Result<Int> = try {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        
        val periods = firestore.collection("rentPeriods")
            .whereEqualTo("tenantId", tenantId)
            .whereIn("status", listOf(RentStatus.UNPAID.name, RentStatus.PARTIALLY_PAID.name, RentStatus.OVERDUE.name))
            .get().await().toObjects(RentPeriod::class.java)

        var remindersSent = 0
        firestore.runBatch { batch ->
            for (period in periods) {
                val diffDays = (period.dueDate - now) / oneDayMs
                
                var title = ""
                var message = ""
                var stage = ""

                if (diffDays in 0..1) {
                    stage = STAGE_DUE_DAY
                    title = "Rent Due"
                    message = "Your rent for ${period.month}/${period.year} is due soon."
                } else if (diffDays < 0) {
                    val overdueDays = -diffDays
                    stage = when {
                        overdueDays >= 7 -> STAGE_OVERDUE_7
                        overdueDays >= 3 -> STAGE_OVERDUE_3
                        else -> STAGE_OVERDUE_1
                    }
                    title = "Rent Overdue"
                    message = "Your rent for ${period.month}/${period.year} is $overdueDays days overdue."
                    if (period.status != RentStatus.OVERDUE) {
                        batch.update(firestore.collection("rentPeriods").document(period.periodId), "status", RentStatus.OVERDUE.name)
                    }
                } else if (diffDays in 2..3) {
                    stage = STAGE_3_DAY
                    title = "Rent Reminder"
                    message = "Your rent for ${period.month}/${period.year} is due in 3 days."
                } else if (diffDays in 6..7) {
                    stage = STAGE_7_DAY
                    title = "Rent Reminder"
                    message = "Your rent for ${period.month}/${period.year} is due in 7 days."
                }

                if (stage.isNotEmpty() && !period.sentReminders.contains(stage)) {
                    val notifRef = firestore.collection("notifications").document()
                    val notification = Notification(
                        notificationId = notifRef.id,
                        tenantId = period.tenantId,
                        boardingHouseId = period.boardingHouseId,
                        title = title,
                        message = message,
                        category = NotificationCategory.RENT
                    )
                    batch.set(notifRef, notification)
                    
                    val updatedSentReminders = period.sentReminders + stage
                    batch.update(firestore.collection("rentPeriods").document(period.periodId), "sentReminders", updatedSentReminders)

                    remindersSent++
                }
            }
        }.await()
        Result.success(remindersSent)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
