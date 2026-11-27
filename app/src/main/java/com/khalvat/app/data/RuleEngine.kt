package com.khalvat.app.data

import android.content.Context
import java.util.Calendar

/** موتور تصمیم‌گیری: آیا این تماس/پیامک الان مشمول یکی از قوانین است؟ */
object RuleEngine {

    data class Decision(
        val ruleName: String,
        val contactName: String?,
        val silence: Boolean,
        val reject: Boolean,
        val hideNotification: Boolean,
        val historyMode: Int
    )

    /**
     * @param candidates شناسه‌های ممکن طرف مقابل: شماره، نام فرستنده یا عنوان اعلان.
     */
    suspend fun decide(
        context: Context,
        db: AppDatabase,
        kind: Int,
        candidates: List<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): Decision? {
        val prefs = Prefs(context)
        if (kind == RuleKind.CALL && !prefs.callsEnabled) return null
        if (kind == RuleKind.SMS && !prefs.smsEnabled) return null

        val matched = db.contacts().getAll().filter { matches(it, candidates) }
        val matchedIds = matched.map { it.id }.toSet()
        val contactName = matched.firstOrNull()?.name

        // حالت تمرکز: همه چیز ساکت، به‌جز مخاطبین ویژه
        if (prefs.focusActive(kind, nowMillis) && matched.none { it.vip }) {
            return Decision(
                ruleName = "حالت تمرکز",
                contactName = contactName,
                silence = true,
                reject = false,
                hideNotification = true,
                historyMode = prefs.focusHistory
            )
        }

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        for (rule in db.rules().getEnabled(kind)) {
            if (!isActive(rule, now)) continue
            val ids = rule.contactIdSet()
            val applies = when (rule.targetMode) {
                TargetMode.ALL -> true
                TargetMode.ONLY_LISTED -> matchedIds.any { it in ids }
                else -> matchedIds.none { it in ids }
            }
            if (applies) {
                return Decision(
                    ruleName = rule.name,
                    contactName = contactName,
                    silence = rule.silence,
                    reject = rule.rejectCall,
                    hideNotification = rule.hideNotification,
                    historyMode = rule.historyMode
                )
            }
        }
        return null
    }

    fun matches(contact: PrivateContact, candidates: List<String>): Boolean =
        candidates.any { raw ->
            val text = raw.trim()
            if (text.isEmpty()) return@any false
            val key = NumberUtil.key(text)
            (key.isNotEmpty() && key == contact.numberKey) ||
                text.equals(contact.name, ignoreCase = true) ||
                text.equals(contact.number, ignoreCase = true)
        }

    /** روز و ساعت و بازه تاریخ قانون را با زمان الان مقایسه می‌کند. */
    fun isActive(rule: Rule, now: Calendar): Boolean {
        val t = now.timeInMillis
        if (rule.dateFrom != 0L && t < rule.dateFrom) return false
        if (rule.dateTo != 0L && t > rule.dateTo) return false

        // Calendar: یکشنبه=۱ ... شنبه=۷  →  شنبه=۰ ... جمعه=۶
        val today = now.get(Calendar.DAY_OF_WEEK) % 7
        val yesterday = (today + 6) % 7

        if (rule.allDay) return rule.dayOn(today)

        val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = rule.startMinute
        val end = rule.endMinute
        return if (start <= end) {
            rule.dayOn(today) && minute in start until end
        } else {
            // بازه‌ای که از نیمه‌شب رد می‌شود، مثلاً ۲۲:۰۰ تا ۰۷:۰۰
            (rule.dayOn(today) && minute >= start) || (rule.dayOn(yesterday) && minute < end)
        }
    }
}
