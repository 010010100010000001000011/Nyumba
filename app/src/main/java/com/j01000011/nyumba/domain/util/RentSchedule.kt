package com.j01000011.nyumba.domain.util

import java.util.Calendar
import java.util.TimeZone

/** Pure scheduling rules for monthly rent, kept free of Firestore so they can be unit tested. */
object RentSchedule {

    /** Due days are limited to 1..28 so every month has the date (no February roll-over). */
    const val MAX_DUE_DAY = 28

    data class Slot(val month: Int, val year: Int, val dueDate: Long)

    /**
     * Returns [count] consecutive monthly slots. The first slot is the next due date on or after
     * the day of [startMillis], so a tenant approved after this month's due day is not born overdue.
     * Due dates are the start (00:00) of the due day in [timeZone].
     */
    fun generate(
        startMillis: Long,
        dueDay: Int,
        count: Int = 12,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<Slot> {
        val day = dueDay.coerceIn(1, MAX_DUE_DAY)
        val start = startOfDay(startMillis, timeZone)
        val first = Calendar.getInstance(timeZone).apply {
            timeInMillis = start
            set(Calendar.DAY_OF_MONTH, day)
            if (timeInMillis < start) add(Calendar.MONTH, 1)
        }
        return (0 until count).map { i ->
            val cal = (first.clone() as Calendar).apply {
                add(Calendar.MONTH, i)
                set(Calendar.DAY_OF_MONTH, day)
            }
            Slot(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR), cal.timeInMillis)
        }
    }

    /** The due date for an existing period's month/year, re-anchored to [dueDay]. */
    fun dueDateFor(month: Int, year: Int, dueDay: Int, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month - 1, dueDay.coerceIn(1, MAX_DUE_DAY), 0, 0, 0)
        }.timeInMillis

    /** Whole calendar days from [now] until [target] (negative when [target] is in the past). */
    fun daysBetween(now: Long, target: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
        val a = startOfDay(now, timeZone)
        val b = startOfDay(target, timeZone)
        // Round to absorb 23/25-hour days around DST changes
        return Math.round((b - a) / 86_400_000.0).toInt()
    }

    fun startOfDay(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

/** Which rent reminder (if any) is due for a period, given what was already sent. */
object RentReminderPolicy {

    enum class Stage(val key: String) {
        SEVEN_DAY("7_DAY"),
        THREE_DAY("3_DAY"),
        DUE_DAY("DUE_DAY"),
        OVERDUE_1("OVERDUE_1"),
        OVERDUE_3("OVERDUE_3"),
        OVERDUE_7("OVERDUE_7")
    }

    data class Reminder(val stage: Stage, val title: String, val message: String, val markOverdue: Boolean)

    /** Stage for a period [daysUntilDue] days away (negative = overdue), or null outside every window. */
    fun stageFor(daysUntilDue: Int): Stage? = when {
        daysUntilDue in 4..7 -> Stage.SEVEN_DAY
        daysUntilDue in 1..3 -> Stage.THREE_DAY
        daysUntilDue == 0 -> Stage.DUE_DAY
        daysUntilDue in -2..-1 -> Stage.OVERDUE_1
        daysUntilDue in -6..-3 -> Stage.OVERDUE_3
        daysUntilDue <= -7 -> Stage.OVERDUE_7
        else -> null
    }

    /**
     * The reminder to send now, or null. A stage is sent at most once, and never after a later
     * stage has gone out (e.g. no "due in 7 days" once "due in 3 days" was sent).
     */
    fun reminderFor(daysUntilDue: Int, sent: Collection<String>, periodLabel: String): Reminder? {
        val stage = stageFor(daysUntilDue) ?: return null
        val alreadyCovered = Stage.entries.filter { it.ordinal >= stage.ordinal }.any { it.key in sent }
        if (alreadyCovered) return null
        val overdue = daysUntilDue < 0
        val (title, message) = when {
            daysUntilDue > 1 -> "Rent reminder" to "Your rent for $periodLabel is due in $daysUntilDue days."
            daysUntilDue == 1 -> "Rent reminder" to "Your rent for $periodLabel is due tomorrow."
            daysUntilDue == 0 -> "Rent due today" to "Your rent for $periodLabel is due today."
            else -> {
                val days = -daysUntilDue
                "Rent overdue" to "Your rent for $periodLabel is $days day${if (days == 1) "" else "s"} overdue."
            }
        }
        return Reminder(stage, title, message, markOverdue = overdue)
    }
}
