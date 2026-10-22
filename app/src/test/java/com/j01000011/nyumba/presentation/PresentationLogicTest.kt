package com.j01000011.nyumba.presentation

import com.j01000011.nyumba.domain.model.RentPeriod
import com.j01000011.nyumba.domain.model.RentStatus
import com.j01000011.nyumba.presentation.payments.detectNetwork
import com.j01000011.nyumba.presentation.tenant.buildTenantStats
import com.j01000011.nyumba.ui.components.formatKwacha
import com.j01000011.nyumba.ui.components.humanize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class PresentationLogicTest {

    @Test
    fun `detects Zambian networks in every common format`() {
        assertEquals("MTN", detectNetwork("0961 234 567"))
        assertEquals("MTN", detectNetwork("+260761234567"))
        assertEquals("Airtel", detectNetwork("0971234567"))
        assertEquals("Airtel", detectNetwork("771234567"))
        assertEquals("Zamtel", detectNetwork("0951234567"))
        assertNull(detectNetwork("0211234567"))   // landline
        assertNull(detectNetwork("097123"))       // too short
        assertNull(detectNetwork(""))
    }

    @Test
    fun `formats kwacha with grouping and two decimals`() {
        assertEquals("K0.00", formatKwacha(0.0))
        assertEquals("K1,250.50", formatKwacha(1250.5))
        assertEquals("K1,000,000.00", formatKwacha(1_000_000.0))
    }

    @Test
    fun `humanizes enum constants`() {
        assertEquals("Partially paid", humanize("PARTIALLY_PAID"))
        assertEquals("Overdue", humanize("OVERDUE"))
    }

    @Test
    fun `dashboard surfaces an overdue month as next due`() {
        val now = Calendar.getInstance().apply { clear(); set(2024, Calendar.NOVEMBER, 20) }.timeInMillis
        fun p(month: Int, status: RentStatus, paid: Double = 0.0) = RentPeriod(
            periodId = "$month", month = month, year = 2024, amountDue = 1000.0, amountPaid = paid, status = status,
            dueDate = Calendar.getInstance().apply { clear(); set(2024, month - 1, 5) }.timeInMillis
        )
        val stats = buildTenantStats(
            listOf(p(12, RentStatus.UNPAID), p(10, RentStatus.PAID, 1000.0), p(11, RentStatus.OVERDUE, 200.0)),
            emptyList(),
            now
        )
        assertEquals(RentStatus.OVERDUE, stats.nextDueStatus)
        assertEquals(800.0, stats.outstandingBalance, 0.0)   // November's remainder; December is not due yet
        assertEquals(1200.0, stats.totalPaid, 0.0)
    }
}
