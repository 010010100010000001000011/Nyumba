package com.j01000011.nyumba.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.j01000011.nyumba.domain.model.Notification
import com.j01000011.nyumba.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    firestore: FirebaseFirestore
) : BaseRepository(firestore), NotificationRepository {

    override fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        try {
            val userSnapshot = firestore.collection("users").document(userId).get().await()
            val roleString = userSnapshot.getString("role")
            val query = if (roleString == "LANDLORD") {
                firestore.collection("notifications")
                    .whereEqualTo("landlordId", userId)
            } else {
                firestore.collection("notifications")
                    .whereEqualTo("tenantId", userId)
            }

            val subscription = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
                    trySend(notifications)
                }
            }
            awaitClose { subscription.remove() }
        } catch (e: Exception) {
            close(e)
        }
    }

    override suspend fun sendNotification(notification: Notification): Result<String> {
        return try {
            val docRef = firestore.collection("notifications").document()
            val newNotification = notification.copy(notificationId = docRef.id)
            docRef.set(newNotification).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val tenantSnapshot = firestore.collection("notifications")
                .whereEqualTo("tenantId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            val landlordSnapshot = firestore.collection("notifications")
                .whereEqualTo("landlordId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            firestore.runBatch { batch ->
                tenantSnapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                landlordSnapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
