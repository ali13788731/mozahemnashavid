package com.khalvat.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.icu.text.DateFormatSymbols
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import java.util.Date
import java.util.Locale

/** Locale-aware formatting. Persian phones get the Jalali calendar and Persian digits. */
object Fmt {
    val isFa: Boolean get() = Locale.getDefault().language == "fa"

    fun ulocale(): ULocale =
        if (isFa) ULocale("fa_IR@calendar=persian") else ULocale.forLocale(Locale.getDefault())

    private fun format(pattern: String, ts: Long): String {
        val text = SimpleDateFormat(pattern, ulocale()).format(Date(ts))
        return if (isFa) text.faDigits() else text
    }

    fun dateTime(ts: Long): String =
        format(if (isFa) "yyyy/MM/dd  HH:mm" else "MMM d, yyyy  HH:mm", ts)

    fun date(ts: Long): String =
        format(if (isFa) "yyyy/MM/dd" else "MMM d, yyyy", ts)

    fun monthYear(ts: Long): String = format("LLLL yyyy", ts)

    fun minutes(total: Int): String {
        val s = "%02d:%02d".format(Locale.US, total / 60, total % 60)
        return if (isFa) s.faDigits() else s
    }

    /** Calendar (Jalali for Persian) positioned at [millis]. */
    fun calendar(millis: Long): Calendar =
        Calendar.getInstance(ulocale()).apply { timeInMillis = millis }

    /** Narrow weekday letters indexed by Calendar.DAY_OF_WEEK (1 = Sunday … 7 = Saturday). */
    fun narrowWeekdays(): Array<String> =
        DateFormatSymbols.getInstance(ulocale())
            .getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.NARROW)

    /** Weekday name for the rule-mask bit (0 = Saturday … 6 = Friday). */
    fun dayName(bit: Int): String {
        val calendarDay = ((bit + 6) % 7) + 1
        return DateFormatSymbols.getInstance(Locale.getDefault())
            .getWeekdays(DateFormatSymbols.FORMAT, DateFormatSymbols.ABBREVIATED)[calendarDay]
    }

    /** Order in which the week is shown: Saturday-first in Persian, Monday-first elsewhere. */
    val dayBitOrder: List<Int>
        get() = if (isFa) listOf(0, 1, 2, 3, 4, 5, 6) else listOf(2, 3, 4, 5, 6, 0, 1)
}

fun String.faDigits(): String =
    map { c -> if (c in '0'..'9') ('۰' + (c - '0')) else c }.joinToString("")

/** Number formatted for the current language. */
fun Int.num(): String = if (Fmt.isFa) toString().faDigits() else toString()

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
