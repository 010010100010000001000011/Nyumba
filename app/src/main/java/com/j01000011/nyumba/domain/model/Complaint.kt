package com.j01000011.nyumba.domain.model

data class Complaint(
    val complaintId: String = "",
    val tenantId: String = "",
    val landlordId: String = "",
    val boardingHouseId: String = "",
    val title: String = "",
    val description: String = "",
    val category: ComplaintCategory = ComplaintCategory.OTHER,
    val status: ComplaintStatus = ComplaintStatus.PENDING,
    val landlordComment: String? = null,
    val localImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val lastReminderAt: Long? = null,
    val reminderStage: Int = 0 // 0, 1 (24h), 2 (48h), 3 (72h)
)

enum class ComplaintCategory {
    PLUMBING,
    ELECTRICITY,
    WATER,
    MAINTENANCE,
    SECURITY,
    NOISE,
    INTERNET,
    OTHER
}

enum class ComplaintStatus {
    PENDING,
    RESOLVED,
    OVERDUE
}
