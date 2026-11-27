package com.khalvat.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object RuleKind {
    const val CALL = 0
    const val SMS = 1
}

object TargetMode {
    /** همه تماس‌ها / پیامک‌ها */
    const val ALL = 0
    /** فقط موارد انتخاب‌شده */
    const val ONLY_LISTED = 1
    /** همه به‌جز موارد انتخاب‌شده (مثلاً بانک و دیجی‌کالا) */
    const val ALL_EXCEPT_LISTED = 2
}

object HistoryMode {
    /** در تاریخچه گوشی بماند */
    const val KEEP = 0
    /** از تاریخچه گوشی حذف و در آرشیو برنامه ذخیره شود (پیامک: کپی در آرشیو) */
    const val ARCHIVE = 1
    /** کاملاً حذف شود، بدون آرشیو (فقط تماس) */
    const val DELETE = 2
}

/** مخاطب یا فرستنده‌ای که کاربر در برنامه ثبت کرده (شماره یا نام فرستنده پیامک مثل Digikala). */
@Entity(
    tableName = "private_contacts",
    indices = [Index(value = ["numberKey"], unique = true)]
)
data class PrivateContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val number: String,
    val numberKey: String,
    /** در حالت تمرکز هم مزاحم می‌شود (استثنا). */
    val vip: Boolean = false
)

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: Int,
    val enabled: Boolean = true,
    val targetMode: Int = TargetMode.ONLY_LISTED,
    /** شناسه مخاطبین انتخاب‌شده، جداشده با کاما. */
    val contactIds: String = "",
    /** بیت ۰ = شنبه ... بیت ۶ = جمعه. ۱۲۷ یعنی هر روز. */
    val daysMask: Int = 127,
    val allDay: Boolean = true,
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 7 * 60,
    /** ۰ یعنی بدون محدودیت تاریخ. */
    val dateFrom: Long = 0,
    val dateTo: Long = 0,
    val silence: Boolean = true,
    val rejectCall: Boolean = false,
    val hideNotification: Boolean = true,
    val historyMode: Int = HistoryMode.ARCHIVE
) {
    fun contactIdSet(): Set<Long> =
        contactIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()

    fun dayOn(index: Int): Boolean = daysMask and (1 shl index) != 0
}

@Entity(tableName = "archived_calls")
data class ArchivedCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val name: String,
    val timestamp: Long,
    val ruleName: String = ""
)

@Entity(tableName = "archived_sms")
data class ArchivedSms(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val name: String,
    val body: String,
    val timestamp: Long,
    val ruleName: String = ""
)
