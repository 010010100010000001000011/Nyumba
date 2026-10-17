package com.j01000011.nyumba.domain.model

import java.util.Calendar

data class RentPeriod(
    val periodId: String = "",
    val tenantId: String = "",
    val boardingHouseId: String = "",
    val roomId: String = "",
    val month: Int = 1, // 1-12
    val year: Int = 2024,
    val amountDue: Double = 0.0,
    val amountPaid: Double = 0.0,
    val status: RentStatus = RentStatus.UNPAID,
    val dueDate: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val sentReminders: List<String> = emptyList() // Tracks stages like "7_DAY", "3_DAY", "DUE_DAY", "OVERDUE_1"
) {
    val remainingBalance: Double get() = maxOf(0.0, amountDue - amountPaid)

    val isCurrentOrPastDue: Boolean
        get() {
            val now = System.currentTimeMillis()
            if (dueDate in 1..now) return true
            if (status == RentStatus.OVERDUE) return true
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val currentYear = cal.get(Calendar.YEAR)
            val currentMonth = cal.get(Calendar.MONTH) + 1
            return year < currentYear || (year == currentYear && month <= currentMonth)
        }
}

enum class RentStatus {
    PAID,
    PARTIALLY_PAID,
    UNPAID,
    ADVANCE_PAID,
    OVERDUE
}
