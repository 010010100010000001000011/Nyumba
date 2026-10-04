package com.j01000011.nyumba.domain.model

data class Receipt(
    val receiptId: String = "",
    val receiptNumber: String = "",
    val paymentId: String = "",
    val tenantId: String = "",
    val landlordId: String = "",
    val boardingHouseId: String = "",
    val roomId: String = "",
    val tenantName: String = "",
    val roomName: String = "",
    val amountPaid: Double = 0.0,
    val previousBalance: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val advanceCoverage: Int = 0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val transactionId: String? = null,
    val paymentDate: Long = 0,
    val generatedAt: Long = System.currentTimeMillis()
)
