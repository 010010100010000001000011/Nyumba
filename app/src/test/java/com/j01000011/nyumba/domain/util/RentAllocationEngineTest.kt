package com.j01000011.nyumba.domain.util

import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RentAllocationEngineTest {

    @Test
    fun `allocatePayment partitions amount correctly across multiple periods`() {
        val periods = listOf(
            RentPeriod(periodId = "1", month = 10, year = 2024, amountDue = 2000.0, amountPaid = 0.0, status = RentStatus.UNPAID),
            RentPeriod(periodId = "2", month = 11, year = 2024, amountDue = 2000.0, amountPaid = 0.0, status = RentStatus.UNPAID),
            RentPeriod(periodId = "3", month = 12, year = 2024, amountDue = 2000.0, amountPaid = 0.0, status = RentStatus.UNPAID)
        )

        // Case 1: Partial payment for first month
        val result1 = RentAllocationEngine.allocatePayment(1300.0, periods)
        assertEquals(1300.0, result1[0].amountPaid, 0.0)
        assertEquals(RentStatus.PARTIALLY_PAID, result1[0].status)
        assertEquals(0.0, result1[1].amountPaid, 0.0)

        // Case 2: Exact payment for two months
        val result2 = RentAllocationEngine.allocatePayment(4000.0, periods)
        assertEquals(2000.0, result2[0].amountPaid, 0.0)
        assertEquals(RentStatus.PAID, result2[0].status)
        assertEquals(2000.0, result2[1].amountPaid, 0.0)
        assertEquals(RentStatus.PAID, result2[1].status)
        assertEquals(0.0, result2[2].amountPaid, 0.0)

        // Case 3: Payment covering advance
        val result3 = RentAllocationEngine.allocatePayment(5500.0, periods)
        assertEquals(2000.0, result3[0].amountPaid, 0.0)
        assertEquals(2000.0, result3[1].amountPaid, 0.0)
        assertEquals(1500.0, result3[2].amountPaid, 0.0)
        assertEquals(RentStatus.PARTIALLY_PAID, result3[2].status)
    }

    @Test
    fun `allocatePayment handles existing partial payments`() {
        val periods = listOf(
            RentPeriod(periodId = "1", month = 10, year = 2024, amountDue = 2000.0, amountPaid = 500.0, status = RentStatus.PARTIALLY_PAID)
        )

        val result = RentAllocationEngine.allocatePayment(1500.0, periods)
        assertEquals(2000.0, result[0].amountPaid, 0.0)
        assertEquals(RentStatus.PAID, result[0].status)
    }
}
