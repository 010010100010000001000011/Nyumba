package com.j01000011.nyumba.domain.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: UserRole = UserRole.TENANT,
    val identificationNumber: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val currentRoomId: String? = null,
    val boardingHouseId: String? = null,
    val landlordId: String? = null,
    val fcmToken: String? = null,
    val monthlyRent: Double = 0.0,
    val paymentDueDay: Int = 1,
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
