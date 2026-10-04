package com.j01000011.nyumba.domain.model

data class Payment(
    val paymentId: String = "",
    val tenantId: String = "",
    val landlordId: String = "",
    val boardingHouseId: String = "",
    val roomId: String = "",
    val amount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val transactionId: String? = null,
    val referenceId: String? = null,
    val paymentDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val notes: String? = null
)

enum class PaymentMethod {
    MOBILE_MONEY,
    CASH
}

enum class PaymentStatus {
    INITIATED,
    PENDING,
    SUCCESSFUL,
    FAILED,
    REVERSED,
    RECORDED
}
