package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.j01000011.nyumba.domain.model.*
import com.j01000011.nyumba.domain.repository.AuditRepository
import com.j01000011.nyumba.domain.repository.ComplaintRepository
import com.j01000011.nyumba.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComplaintRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val auditRepository: AuditRepository
) : BaseRepository(firestore), ComplaintRepository {

    override fun getTenantComplaints(tenantId: String): Flow<List<Complaint>> = callbackFlow {
        val subscription = firestore.collection("complaints")
            .whereEqualTo("tenantId", tenantId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Complaint::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun getHouseComplaints(boardingHouseId: String): Flow<List<Complaint>> = callbackFlow {
        val subscription = firestore.collection("complaints")
            .whereEqualTo("boardingHouseId", boardingHouseId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Complaint::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun getComplaintById(complaintId: String): Result<Complaint> = try {
        val snapshot = firestore.collection("complaints").document(complaintId).get().await()
        val complaint = snapshot.toObject(Complaint::class.java) ?: throw Exception("Complaint not found")
        Result.success(complaint)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun submitComplaint(complaint: Complaint): Result<String> = try {
        val ref = firestore.collection("complaints").document()
        val newComplaint = complaint.copy(complaintId = ref.id)
        ref.set(newComplaint).await()

        // Notify landlord
        notificationRepository.sendNotification(
            Notification(
                landlordId = complaint.landlordId,
                boardingHouseId = complaint.boardingHouseId,
                title = "New Complaint",
                message = "A new ${complaint.category.name} complaint has been submitted: ${complaint.title}",
                category = NotificationCategory.COMPLAINT
            )
        )

        auditRepository.recordLog(AuditLog(
            action = "COMPLAINT_CREATED",
            entityType = "COMPLAINT",
            entityId = ref.id,
            boardingHouseId = complaint.boardingHouseId,
            metadata = mapOf("category" to complaint.category.name, "title" to complaint.title)
        ))

        Result.success(ref.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateComplaintStatus(
        complaintId: String,
        status: String,
        comment: String?
    ): Result<Unit> = try {
        val now = System.currentTimeMillis()
        val updateMap = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to now
        )
        if (status == ComplaintStatus.RESOLVED.name) {
            updateMap["resolvedAt"] = now
        }
        if (comment != null) {
            updateMap["landlordComment"] = comment
        }

        firestore.collection("complaints").document(complaintId).update(updateMap).await()

        // Notify tenant
        val complaint = getComplaintById(complaintId).getOrNull()
        if (complaint != null) {
            notificationRepository.sendNotification(
                Notification(
                    tenantId = complaint.tenantId,
                    boardingHouseId = complaint.boardingHouseId,
                    title = "Complaint Updated",
                    message = "Your complaint '${complaint.title}' is now $status.",
                    category = NotificationCategory.COMPLAINT
                )
            )

            auditRepository.recordLog(AuditLog(
                action = "COMPLAINT_STATUS_UPDATED",
                entityType = "COMPLAINT",
                entityId = complaintId,
                boardingHouseId = complaint.boardingHouseId,
                metadata = mapOf("status" to status)
            ))
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun checkAndSendComplaintReminders(): Result<Int> = try {
        val now = System.currentTimeMillis()
        val pendingComplaints = firestore.collection("complaints")
            .whereEqualTo("status", ComplaintStatus.PENDING.name)
            .get().await().toObjects(Complaint::class.java)

        var remindersSent = 0

        firestore.runBatch { batch ->
            for (complaint in pendingComplaints) {
                val ageMs = now - complaint.createdAt
                val hoursAge = ageMs / (1000 * 60 * 60)
                
                var newStage = 0
                var sendReminder = false

                if (hoursAge >= 72 && complaint.reminderStage < 3) {
                    newStage = 3
                    sendReminder = true
                } else if (hoursAge >= 48 && complaint.reminderStage < 2) {
                    newStage = 2
                    sendReminder = true
                } else if (hoursAge >= 24 && complaint.reminderStage < 1) {
                    newStage = 1
                    sendReminder = true
                }

                if (sendReminder) {
                    val ref = firestore.collection("complaints").document(complaint.complaintId)
                    batch.update(ref, "reminderStage", newStage, "lastReminderAt", now)
                    
                    if (newStage == 3) {
                        batch.update(ref, "status", ComplaintStatus.OVERDUE.name)
                    }

                    val notifRef = firestore.collection("notifications").document()
                    val reminderNotif = Notification(
                        notificationId = notifRef.id,
                        landlordId = complaint.landlordId,
                        boardingHouseId = complaint.boardingHouseId,
                        title = if (newStage == 3) "URGENT: Overdue Complaint" else "Complaint Reminder ($newStage)",
                        message = "The complaint '${complaint.title}' has been pending for $hoursAge hours.",
                        category = NotificationCategory.COMPLAINT
                    )
                    batch.set(notifRef, reminderNotif)
                    remindersSent++
                }
            }
        }.await()

        Result.success(remindersSent)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
