package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.AuditRepository
import com.j01000011.nyumba.domain.repository.NotificationRepository
import com.j01000011.nyumba.domain.repository.RentRepository
import com.j01000011.nyumba.domain.util.RentAllocationEngine
import com.j01000011.nyumba.domain.util.RentReminderPolicy
import com.j01000011.nyumba.domain.util.RentSchedule
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
        require(payment.amount > 0) { "Amount must be greater than zero" }
        val now = System.currentTimeMillis()

        // Scope to this house: the tenant may have periods from an earlier tenancy elsewhere
        val periods = firestore.collection("rentPeriods")
            .whereEqualTo("tenantId", payment.tenantId)
            .whereEqualTo("boardingHouseId", payment.boardingHouseId)
            .get().await().toObjects(RentPeriod::class.java)
            .filter { it.status != RentStatus.PAID && it.status != RentStatus.ADVANCE_PAID }

        if (periods.isEmpty()) throw IllegalStateException("This tenant has no unpaid rent to apply the payment to")
        val scheduleBalance = RentAllocationEngine.scheduleBalance(periods)
        if (payment.amount > scheduleBalance) {
            throw IllegalArgumentException(
                "Amount is more than the remaining rent schedule (maximum K${"%.2f".format(Locale.US, scheduleBalance)})"
            )
        }

        val allocation = RentAllocationEngine.allocate(payment.amount, periods, now)
        val previousBalance = RentAllocationEngine.dueBalance(periods, now)
        val remainingBalance = RentAllocationEngine.dueBalance(allocation.periods, now)

        val tenantName = firestore.collection("users").document(payment.tenantId).get().await().getString("fullName") ?: "Tenant"
        val roomName = firestore.collection("rooms").document(payment.roomId).get().await().getString("roomName") ?: "Room"

        val paymentRef = firestore.collection("payments").document()
        val receiptRef = firestore.collection("receipts").document()
        val receipt = Receipt(
            receiptId = receiptRef.id,
            receiptNumber = "REC-${now / 1000}",
            paymentId = paymentRef.id,
            tenantId = payment.tenantId,
            landlordId = payment.landlordId,
            boardingHouseId = payment.boardingHouseId,
            roomId = payment.roomId,
            tenantName = tenantName,
            roomName = roomName,
            amountPaid = payment.amount,
            previousBalance = previousBalance,
            remainingBalance = remainingBalance,
            advanceCoverage = allocation.advanceMonths,
            paymentMethod = payment.paymentMethod,
            transactionId = payment.transactionId,
            paymentDate = payment.paymentDate,
            generatedAt = now
        )

        // Payment, rent allocation and receipt succeed or fail together
        firestore.runBatch { batch ->
            batch.set(paymentRef, payment.copy(paymentId = paymentRef.id, status = PaymentStatus.SUCCESSFUL))
            for (period in allocation.changed) {
                batch.update(
                    firestore.collection("rentPeriods").document(period.periodId),
                    "amountPaid", period.amountPaid,
                    "status", period.status.name,
                    "updatedAt", now
                )
            }
            batch.set(receiptRef, receipt)
        }.await()

        notificationRepository.sendNotification(
            Notification(
                tenantId = payment.tenantId,
                boardingHouseId = payment.boardingHouseId,
                title = "Payment received",
                message = "Your payment of K${"%.2f".format(Locale.US, payment.amount)} was recorded. Your receipt is ready.",
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
            val slots = RentSchedule.generate(System.currentTimeMillis(), tenant.paymentDueDay)
            firestore.runBatch { batch ->
                for (slot in slots) {
                    val ref = firestore.collection("rentPeriods").document()
                    batch.set(ref, RentPeriod(
                        periodId = ref.id,
                        tenantId = tenant.uid,
                        boardingHouseId = tenant.boardingHouseId ?: "",
                        roomId = tenant.currentRoomId ?: "",
                        month = slot.month,
                        year = slot.year,
                        amountDue = tenant.monthlyRent,
                        status = RentStatus.UNPAID,
                        dueDate = slot.dueDate
                    ))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rescheduleFuturePeriods(
        tenantId: String,
        boardingHouseId: String,
        monthlyRent: Double,
        dueDay: Int
    ): Result<Int> = try {
        val now = System.currentTimeMillis()
        val untouched = firestore.collection("rentPeriods")
            .whereEqualTo("tenantId", tenantId)
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .get().await().toObjects(RentPeriod::class.java)
            .filter { it.status == RentStatus.UNPAID && it.amountPaid == 0.0 && it.dueDate > now }

        firestore.runBatch { batch ->
            for (period in untouched) {
                batch.update(
                    firestore.collection("rentPeriods").document(period.periodId),
                    "amountDue", monthlyRent,
                    "dueDate", RentSchedule.dueDateFor(period.month, period.year, dueDay),
                    "updatedAt", now
                )
            }
        }.await()
        Result.success(untouched.size)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkAndSendRentReminders(tenantId: String): Result<Int> = try {
        val now = System.currentTimeMillis()
        val periods = firestore.collection("rentPeriods")
            .whereEqualTo("tenantId", tenantId)
            .whereIn("status", listOf(RentStatus.UNPAID.name, RentStatus.PARTIALLY_PAID.name, RentStatus.OVERDUE.name))
            .get().await().toObjects(RentPeriod::class.java)

        var remindersSent = 0
        firestore.runBatch { batch ->
            for (period in periods) {
                val daysUntilDue = RentSchedule.daysBetween(now, period.dueDate)
                val periodRef = firestore.collection("rentPeriods").document(period.periodId)

                if (daysUntilDue < 0 && period.status != RentStatus.OVERDUE) {
                    batch.update(periodRef, "status", RentStatus.OVERDUE.name)
                }

                val reminder = RentReminderPolicy.reminderFor(daysUntilDue, period.sentReminders, "${period.month}/${period.year}")
                    ?: continue
                val notifRef = firestore.collection("notifications").document()
                batch.set(notifRef, Notification(
                    notificationId = notifRef.id,
                    tenantId = period.tenantId,
                    boardingHouseId = period.boardingHouseId,
                    title = reminder.title,
                    message = reminder.message,
                    category = NotificationCategory.RENT
                ))
                batch.update(periodRef, "sentReminders", period.sentReminders + reminder.stage.key)
                remindersSent++
            }
        }.await()
        Result.success(remindersSent)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
