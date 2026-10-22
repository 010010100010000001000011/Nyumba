package com.j01000011.nyumba.domain.util

import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.domain.model.RoomStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RentAllocationEngineTest {

    // "Now" is 15 Oct 2024 for every test
    private val now = Calendar.getInstance().apply { clear(); set(2024, Calendar.OCTOBER, 15, 12, 0) }.timeInMillis

    private fun period(id: String, month: Int, year: Int = 2024, paid: Double = 0.0) = RentPeriod(
        periodId = id, month = month, year = year, amountDue = 2000.0, amountPaid = paid,
        status = if (paid > 0) RentStatus.PARTIALLY_PAID else RentStatus.UNPAID
    )

    private val periods = listOf(period("1", 10), period("2", 11), period("3", 12))

    @Test
    fun `partial payment for first month`() {
        val r = RentAllocationEngine.allocate(1300.0, periods, now)
        assertEquals(1300.0, r.periods[0].amountPaid, 0.0)
        assertEquals(RentStatus.PARTIALLY_PAID, r.periods[0].status)
        assertEquals(0.0, r.periods[1].amountPaid, 0.0)
        assertEquals(listOf("1"), r.changed.map { it.periodId })
    }

    @Test
    fun `current month is PAID while future months become ADVANCE_PAID`() {
        val r = RentAllocationEngine.allocate(4000.0, periods, now)
        assertEquals(RentStatus.PAID, r.periods[0].status)          // October (current)
        assertEquals(RentStatus.ADVANCE_PAID, r.periods[1].status)  // November (future)
        assertEquals(1, r.advanceMonths)
        assertEquals(0.0, r.periods[2].amountPaid, 0.0)
    }

    @Test
    fun `existing partial payment is topped up`() {
        val r = RentAllocationEngine.allocate(1500.0, listOf(period("1", 10, paid = 500.0)), now)
        assertEquals(2000.0, r.periods[0].amountPaid, 0.0)
        assertEquals(RentStatus.PAID, r.periods[0].status)
    }

    @Test
    fun `oldest month is paid first regardless of input order`() {
        val r = RentAllocationEngine.allocate(2000.0, listOf(period("b", 11), period("a", 9)), now)
        assertEquals(listOf("a"), r.changed.map { it.periodId })
    }

    @Test
    fun `overpayment beyond the schedule is reported, not lost`() {
        val r = RentAllocationEngine.allocate(7000.5, periods, now)
        assertEquals(1000.5, r.unallocated, 0.001)
        assertTrue(r.periods.all { it.amountPaid == 2000.0 })
    }

    @Test
    fun `cents do not accumulate floating point error`() {
        val r = RentAllocationEngine.allocate(0.3, listOf(period("1", 10).copy(amountDue = 0.3)), now)
        assertEquals(RentStatus.PAID, r.periods[0].status)
        assertEquals(0.0, r.unallocated, 0.0)
    }

    @Test
    fun `due balance excludes future months but schedule balance includes them`() {
        assertEquals(2000.0, RentAllocationEngine.dueBalance(periods, now), 0.0)
        assertEquals(6000.0, RentAllocationEngine.scheduleBalance(periods), 0.0)
    }

    @Test
    fun `room approval accounts for other tenants holds`() {
        // 2 beds, 1 occupied, 1 held by someone else: a PENDING request must not overbook
        assertFalse(RoomCapacity.canApprove(capacity = 2, occupied = 1, held = 1, requestIsHolding = false))
        // The holder of that bed can be approved
        assertTrue(RoomCapacity.canApprove(capacity = 2, occupied = 1, held = 1, requestIsHolding = true))
        assertEquals(0, RoomCapacity.freeBeds(2, 1, 1))
        assertFalse(RoomCapacity.canApprove(capacity = 1, occupied = 1, held = 0, requestIsHolding = false))
    }

    @Test
    fun `occupancy changes keep manual room statuses`() {

        assertEquals(RoomStatus.MAINTENANCE, RoomCapacity.statusAfterChange(RoomStatus.MAINTENANCE, occupied = 0, capacity = 2))
        assertEquals(RoomStatus.INACTIVE, RoomCapacity.statusAfterChange(RoomStatus.INACTIVE, occupied = 2, capacity = 2))
        assertEquals(RoomStatus.FULL, RoomCapacity.statusAfterChange(RoomStatus.AVAILABLE, occupied = 2, capacity = 2))
        assertEquals(RoomStatus.AVAILABLE, RoomCapacity.statusAfterChange(RoomStatus.FULL, occupied = 1, capacity = 2))
    }
}
