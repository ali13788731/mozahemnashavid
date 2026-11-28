package com.khalvat.app.ui

import android.content.Context
import com.khalvat.app.R
import com.khalvat.app.data.PrivateContact
import com.khalvat.app.data.Rule
import com.khalvat.app.data.RuleKind
import com.khalvat.app.data.TargetMode

/** Short human-readable summaries shown on rule cards. */
object RuleText {

    fun days(ctx: Context, mask: Int): String = when (mask) {
        127 -> ctx.getString(R.string.days_every)
        0 -> ctx.getString(R.string.days_none)
        else -> Fmt.dayBitOrder.filter { mask and (1 shl it) != 0 }
            .joinToString(", ") { Fmt.dayName(it) }
    }

    fun time(ctx: Context, rule: Rule): String =
        if (rule.allDay) ctx.getString(R.string.all_day)
        else "${Fmt.minutes(rule.startMinute)} – ${Fmt.minutes(rule.endMinute)}"

    fun dates(ctx: Context, rule: Rule): String? = when {
        rule.dateFrom == 0L && rule.dateTo == 0L -> null
        rule.dateFrom != 0L && rule.dateTo != 0L ->
            if (Fmt.date(rule.dateFrom) == Fmt.date(rule.dateTo))
                ctx.getString(R.string.dates_only, Fmt.date(rule.dateFrom))
            else ctx.getString(R.string.dates_range, Fmt.date(rule.dateFrom), Fmt.date(rule.dateTo))
        rule.dateFrom != 0L -> ctx.getString(R.string.dates_from, Fmt.date(rule.dateFrom))
        else -> ctx.getString(R.string.dates_until, Fmt.date(rule.dateTo))
    }

    fun schedule(ctx: Context, rule: Rule): String {
        val parts = mutableListOf(days(ctx, rule.daysMask), time(ctx, rule))
        dates(ctx, rule)?.let { parts += it }
        return parts.joinToString(" • ")
    }

    fun target(ctx: Context, rule: Rule, contacts: List<PrivateContact>): String {
        val ids = rule.contactIdSet()
        val names = contacts.filter { it.id in ids }.map { it.name }
        val list = when {
            names.isEmpty() -> "—"
            names.size <= 3 -> names.joinToString(", ")
            else -> ctx.getString(R.string.tgt_more, names.take(3).joinToString(", "), (names.size - 3).num())
        }
        val all = ctx.getString(if (rule.kind == RuleKind.CALL) R.string.who_all_calls else R.string.who_all_sms)
        return when (rule.targetMode) {
            TargetMode.ALL -> all
            TargetMode.ONLY_LISTED -> ctx.getString(R.string.tgt_only, list)
            else -> ctx.getString(R.string.tgt_except, list)
        }
    }

    fun behavior(ctx: Context, rule: Rule): String =
        if (rule.kind == RuleKind.CALL) {
            val parts = mutableListOf<String>()
            parts += ctx.getString(
                when {
                    rule.rejectCall -> R.string.beh_reject
                    rule.silence -> R.string.beh_silence
                    else -> R.string.beh_ring
                }
            )
            if (rule.hideNotification) parts += ctx.getString(R.string.beh_no_notif)
            parts.joinToString(" + ")
        } else {
            ctx.getString(if (rule.hideNotification) R.string.beh_hide_notif else R.string.beh_show_notif)
        }
}
