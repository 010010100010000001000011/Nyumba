package com.j01000011.nyumba.domain.util

import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus

object RentAllocationEngine {

    /**
     * Allocates a payment amount across a list of outstanding rent periods.
     * Returns a list of updated periods.
     */
    fun allocatePayment(
        paymentAmount: Double,
        unpaidPeriods: List<RentPeriod>
    ): List<RentPeriod> {
        var remainingAmount = paymentAmount
        val updatedPeriods = mutableListOf<RentPeriod>()

        // Sort by year and month to ensure oldest is paid first
        val sortedPeriods = unpaidPeriods.sortedWith(compareBy({ it.year }, { it.month }))

        for (period in sortedPeriods) {
            if (remainingAmount <= 0) {
                updatedPeriods.add(period)
                continue
            }

            val balance = period.amountDue - period.amountPaid
            if (balance <= 0) {
                updatedPeriods.add(period)
                continue
            }

            val toApply = minOf(remainingAmount, balance)
            val newPaid = period.amountPaid + toApply
            remainingAmount -= toApply

            val newStatus = if (newPaid >= period.amountDue) RentStatus.PAID else RentStatus.PARTIALLY_PAID
            
            updatedPeriods.add(
                period.copy(
                    amountPaid = newPaid,
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        return updatedPeriods
    }
}
