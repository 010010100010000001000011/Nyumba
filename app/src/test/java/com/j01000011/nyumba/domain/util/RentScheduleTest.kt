package com.j01000011.nyumba.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RentScheduleTest {

    private val tz: TimeZone = TimeZone.getTimeZone("Africa/Lusaka")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        Calendar.getInstance(tz).apply { clear(); set(year, month - 1, day, hour, 0, 0) }.timeInMillis

    private fun Long.ymd(): Triple<Int, Int, Int> = Calendar.getInstance(tz).let {
        it.timeInMillis = this
        Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `first period is this month when approved before the due day`() {
        val slots = RentSchedule.generate(at(2026, 10, 3), dueDay = 5, timeZone = tz)
        assertEquals(Triple(2026, 10, 5), slots.first().dueDate.ymd())
        assertEquals(10, slots.first().month)
    }

    @Test
    fun `first period is this month when approved on the due day`() {
        val slots = RentSchedule.generate(at(2026, 10, 5, hour = 18), dueDay = 5, timeZone = tz)
        assertEquals(Triple(2026, 10, 5), slots.first().dueDate.ymd())
    }

    @Test
    fun `tenant approved after the due day is not born overdue`() {
        val slots = RentSchedule.generate(at(2026, 10, 22), dueDay = 5, timeZone = tz)
        assertEquals(Triple(2026, 11, 5), slots.first().dueDate.ymd())
        assertTrue(slots.first().dueDate > at(2026, 10, 22))
    }

    @Test
    fun `due day 31 is capped so February does not roll into March`() {
        val slots = RentSchedule.generate(at(2027, 1, 1), dueDay = 31, count = 3, timeZone = tz)
        assertEquals(listOf(Triple(2027, 1, 28), Triple(2027, 2, 28), Triple(2027, 3, 28)), slots.map { it.dueDate.ymd() })
        assertEquals(listOf(1, 2, 3), slots.map { it.month })
    }

    @Test
    fun `twelve consecutive months across a year boundary`() {
        val slots = RentSchedule.generate(at(2026, 11, 1), dueDay = 1, timeZone = tz)
        assertEquals(12, slots.size)
        assertEquals((11 to 2026), slots[0].month to slots[0].year)
        assertEquals((1 to 2027), slots[2].month to slots[2].year)
        assertEquals((10 to 2027), slots[11].month to slots[11].year)
        slots.zipWithNext().forEach { (a, b) -> assertTrue(b.dueDate > a.dueDate) }
    }

    @Test
    fun `daysBetween counts calendar days not 24h blocks`() {
        assertEquals(0, RentSchedule.daysBetween(at(2026, 10, 5, 23), at(2026, 10, 5, 0), tz))
        assertEquals(1, RentSchedule.daysBetween(at(2026, 10, 5, 23), at(2026, 10, 6, 0), tz))
        assertEquals(-3, RentSchedule.daysBetween(at(2026, 10, 8, 1), at(2026, 10, 5, 0), tz))
    }

    @Test
    fun `reminder windows are wide enough for a twelve hourly job`() {
        assertEquals(RentReminderPolicy.Stage.SEVEN_DAY, RentReminderPolicy.stageFor(7))
        assertEquals(RentReminderPolicy.Stage.SEVEN_DAY, RentReminderPolicy.stageFor(4))
        assertEquals(RentReminderPolicy.Stage.THREE_DAY, RentReminderPolicy.stageFor(3))
        assertEquals(RentReminderPolicy.Stage.THREE_DAY, RentReminderPolicy.stageFor(1))
        assertEquals(RentReminderPolicy.Stage.DUE_DAY, RentReminderPolicy.stageFor(0))
        assertEquals(RentReminderPolicy.Stage.OVERDUE_1, RentReminderPolicy.stageFor(-1))
        assertEquals(RentReminderPolicy.Stage.OVERDUE_3, RentReminderPolicy.stageFor(-5))
        assertEquals(RentReminderPolicy.Stage.OVERDUE_7, RentReminderPolicy.stageFor(-40))
        assertNull(RentReminderPolicy.stageFor(8))
    }

    @Test
    fun `reminder message states the real number of days`() {
        val r = RentReminderPolicy.reminderFor(2, emptyList(), "10/2026")!!
        assertEquals("Your rent for 10/2026 is due in 2 days.", r.message)
        assertEquals("Your rent for 10/2026 is due tomorrow.", RentReminderPolicy.reminderFor(1, emptyList(), "10/2026")!!.message)
        val overdue = RentReminderPolicy.reminderFor(-1, emptyList(), "10/2026")!!
        assertEquals("Your rent for 10/2026 is 1 day overdue.", overdue.message)
        assertTrue(overdue.markOverdue)
    }

    @Test
    fun `each stage is sent once and earlier stages are skipped after later ones`() {
        assertNull(RentReminderPolicy.reminderFor(3, listOf("3_DAY"), "x"))
        // 7-day window but the 3-day reminder already went out: nothing
        assertNull(RentReminderPolicy.reminderFor(5, listOf("3_DAY"), "x"))
        // Moving into a later window sends the new stage
        assertEquals(RentReminderPolicy.Stage.DUE_DAY, RentReminderPolicy.reminderFor(0, listOf("7_DAY", "3_DAY"), "x")!!.stage)
    }
}
