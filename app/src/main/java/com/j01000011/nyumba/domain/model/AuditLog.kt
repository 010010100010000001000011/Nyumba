package com.j01000011.nyumba.domain.model

data class AuditLog(
    val logId: String = "",
    val actorId: String = "",
    val actorRole: UserRole = UserRole.LANDLORD,
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val boardingHouseId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)
