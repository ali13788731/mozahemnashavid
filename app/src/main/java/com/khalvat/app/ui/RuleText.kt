package com.khalvat.app.ui

import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.PrivateContact
import com.khalvat.app.data.Rule
import com.khalvat.app.data.RuleKind
import com.khalvat.app.data.TargetMode

/** متن‌های خلاصه‌ی هر قانون برای نمایش در کارت‌ها. */
object RuleText {
    val dayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    fun days(mask: Int): String = when (mask) {
        127 -> "هر روز"
        0 -> "هیچ روز"
        else -> dayNames.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString("، ")
    }

    fun time(rule: Rule): String =
        if (rule.allDay) "تمام روز"
        else "${Fmt.minutes(rule.startMinute)} تا ${Fmt.minutes(rule.endMinute)}"

    fun dates(rule: Rule): String? = when {
        rule.dateFrom == 0L && rule.dateTo == 0L -> null
        rule.dateFrom != 0L && rule.dateTo != 0L ->
            if (Fmt.date(rule.dateFrom) == Fmt.date(rule.dateTo)) "فقط ${Fmt.date(rule.dateFrom)}"
            else "از ${Fmt.date(rule.dateFrom)} تا ${Fmt.date(rule.dateTo)}"
        rule.dateFrom != 0L -> "از ${Fmt.date(rule.dateFrom)}"
        else -> "تا ${Fmt.date(rule.dateTo)}"
    }

    fun schedule(rule: Rule): String {
        val parts = mutableListOf(days(rule.daysMask), time(rule))
        dates(rule)?.let { parts += it }
        return parts.joinToString(" • ")
    }

    fun target(rule: Rule, contacts: List<PrivateContact>): String {
        val ids = rule.contactIdSet()
        val names = contacts.filter { it.id in ids }.map { it.name }
        val list = when {
            names.isEmpty() -> "—"
            names.size <= 3 -> names.joinToString("، ")
            else -> names.take(3).joinToString("، ") + " و ${(names.size - 3).fa()} مورد دیگر"
        }
        val all = if (rule.kind == RuleKind.CALL) "همه تماس‌ها" else "همه پیامک‌ها"
        return when (rule.targetMode) {
            TargetMode.ALL -> all
            TargetMode.ONLY_LISTED -> "فقط: $list"
            else -> "$all به‌جز: $list"
        }
    }

    fun behavior(rule: Rule): String =
        if (rule.kind == RuleKind.CALL) {
            val parts = mutableListOf<String>()
            parts += when {
                rule.rejectCall -> "رد تماس"
                rule.silence -> "بی‌صدا"
                else -> "بدون قطع صدا"
            }
            if (rule.hideNotification) parts += "بدون اعلان"
            parts.joinToString(" + ")
        } else {
            if (rule.hideNotification) "اعلان پنهان" else "اعلان نمایش داده می‌شود"
        }

    fun history(rule: Rule): String =
        if (rule.kind == RuleKind.CALL) {
            when (rule.historyMode) {
                HistoryMode.KEEP -> "در تاریخچه گوشی می‌ماند"
                HistoryMode.ARCHIVE -> "حذف از گوشی + آرشیو در برنامه"
                else -> "کاملاً حذف می‌شود"
            }
        } else {
            if (rule.historyMode == HistoryMode.ARCHIVE) "کپی در آرشیو برنامه" else "بدون کپی در آرشیو"
        }
}
