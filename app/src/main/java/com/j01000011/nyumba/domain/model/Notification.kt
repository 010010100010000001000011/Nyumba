package com.j01000011.nyumba.domain.model

import com.google.firebase.firestore.PropertyName

data class Notification(
    val notificationId: String = "",
    val tenantId: String? = null,
    val landlordId: String? = null,
    val boardingHouseId: String? = null,
    val title: String = "",
    val message: String = "",
    val category: NotificationCategory = NotificationCategory.GENERAL,
    // Without @PropertyName, Firestore stores Kotlin "isX" booleans as "x" and never reads them back
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationCategory {
    RENT,
    PAYMENT,
    COMPLAINT,
    ROOM_ASSIGNMENT,
    MAINTENANCE,
    GENERAL,
    SYSTEM,
    MESSAGE
}
