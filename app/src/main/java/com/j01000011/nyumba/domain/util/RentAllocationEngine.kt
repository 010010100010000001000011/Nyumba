package com.j01000011.nyumba.domain.util

import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus

object RentAllocationEngine {

    data class Allocation(
        /** Every input period, updated where money was applied. */
        val periods: List<RentPeriod>,
        /** Only the periods whose amountPaid/status changed. */
        val changed: List<RentPeriod>,
        /** Money left after every period was settled (should be prevented upstream). */
        val unallocated: Double
    ) {
        /** Months settled ahead of time by this payment. */
        val advanceMonths: Int get() = changed.count { it.status == RentStatus.ADVANCE_PAID }
    }

    /**
     * Allocates a payment across outstanding rent periods, oldest first.
     * A period fully paid before its month arrives is marked ADVANCE_PAID; otherwise PAID.
     */
    fun allocate(paymentAmount: Double, unpaidPeriods: List<RentPeriod>, now: Long = System.currentTimeMillis()): Allocation {
        var remaining = round2(paymentAmount)
        val periods = mutableListOf<RentPeriod>()
        val changed = mutableListOf<RentPeriod>()

        for (period in unpaidPeriods.sortedWith(compareBy({ it.year }, { it.month }))) {
            val balance = round2(period.amountDue - period.amountPaid)
            if (remaining <= 0 || balance <= 0) {
                periods.add(period)
                continue
            }
            val toApply = minOf(remaining, balance)
            val newPaid = round2(period.amountPaid + toApply)
            remaining = round2(remaining - toApply)

            val newStatus = when {
                newPaid < period.amountDue -> RentStatus.PARTIALLY_PAID
                isFutureMonth(period, now) -> RentStatus.ADVANCE_PAID
                else -> RentStatus.PAID
            }
            val updated = period.copy(amountPaid = newPaid, status = newStatus, updatedAt = now)
            periods.add(updated)
            changed.add(updated)
        }
        return Allocation(periods, changed, maxOf(0.0, remaining))
    }

    /** Backwards-compatible wrapper returning just the updated periods. */
    fun allocatePayment(paymentAmount: Double, unpaidPeriods: List<RentPeriod>): List<RentPeriod> =
        allocate(paymentAmount, unpaidPeriods).periods

    /** Outstanding rent that is already due (current or past months only). */
    fun dueBalance(periods: List<RentPeriod>, now: Long = System.currentTimeMillis()): Double =
        round2(periods.filter { !isFutureMonth(it, now) }.sumOf { maxOf(0.0, it.amountDue - it.amountPaid) })

    /** Everything still owed on the schedule, including future months. */
    fun scheduleBalance(periods: List<RentPeriod>): Double =
        round2(periods.sumOf { maxOf(0.0, it.amountDue - it.amountPaid) })

    private fun isFutureMonth(period: RentPeriod, now: Long): Boolean {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val y = cal.get(java.util.Calendar.YEAR)
        val m = cal.get(java.util.Calendar.MONTH) + 1
        return period.year > y || (period.year == y && period.month > m)
    }

    private fun round2(v: Double): Double = Math.round(v * 100) / 100.0
}

/** Room capacity rules shared by holds, approvals and room changes. */
object RoomCapacity {
    /** Beds free for a new hold or a new (non-holding) request. */
    fun freeBeds(capacity: Int, occupied: Int, held: Int): Int = maxOf(0, capacity - occupied - held)

    /**
     * Whether a request can be approved. A HOLDING request already owns one of the held beds;
     * a PENDING one must fit in the beds nobody occupies or holds.
     */
    fun canApprove(capacity: Int, occupied: Int, held: Int, requestIsHolding: Boolean): Boolean {
        val othersHeld = if (requestIsHolding) maxOf(0, held - 1) else held
        return occupied + othersHeld < capacity
    }

    /** Status after occupancy changes: MAINTENANCE/INACTIVE are manual and kept; otherwise FULL or AVAILABLE. */
    fun statusAfterChange(current: com.j01000011.nyumba.domain.model.RoomStatus, occupied: Int, capacity: Int) = when (current) {
        com.j01000011.nyumba.domain.model.RoomStatus.MAINTENANCE, com.j01000011.nyumba.domain.model.RoomStatus.INACTIVE -> current
        else -> if (occupied >= capacity) com.j01000011.nyumba.domain.model.RoomStatus.FULL else com.j01000011.nyumba.domain.model.RoomStatus.AVAILABLE
    }
}
