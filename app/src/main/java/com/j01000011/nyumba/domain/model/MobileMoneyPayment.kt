package com.j01000011.nyumba.domain.model

data class MobileMoneyPayment(
    val transactionId: String = "",
    val phoneNumber: String = "",
    val amount: Double = 0.0,
    val status: MobileMoneyStatus = MobileMoneyStatus.PENDING,
    val provider: String = "Lipila",
    val message: String? = null,
    val receiptId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MobileMoneyStatus {
    PENDING,
    SUCCESSFUL,
    FAILED,
    CANCELLED
}
