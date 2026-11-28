package com.khalvat.app.data

import android.content.Context

/** App settings (non-sensitive). */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("khalvat_settings", Context.MODE_PRIVATE)

    var blockScreenshots: Boolean
        get() = sp.getBoolean("blockScreenshots", true)
        set(v) = sp.edit().putBoolean("blockScreenshots", v).apply()

    var callsEnabled: Boolean
        get() = sp.getBoolean("callsEnabled", true)
        set(v) = sp.edit().putBoolean("callsEnabled", v).apply()

    var smsEnabled: Boolean
        get() = sp.getBoolean("smsEnabled", true)
        set(v) = sp.edit().putBoolean("smsEnabled", v).apply()

    /** زمان پایان حالت تمرکز؛ ۰ یعنی غیرفعال. */
    var focusUntil: Long
        get() = sp.getLong("focusUntil", 0L)
        set(v) = sp.edit().putLong("focusUntil", v).apply()

    var focusCalls: Boolean
        get() = sp.getBoolean("focusCalls", true)
        set(v) = sp.edit().putBoolean("focusCalls", v).apply()

    var focusSms: Boolean
        get() = sp.getBoolean("focusSms", true)
        set(v) = sp.edit().putBoolean("focusSms", v).apply()

    var focusHistory: Int
        get() = sp.getInt("focusHistory", HistoryMode.ARCHIVE)
        set(v) = sp.edit().putInt("focusHistory", v).apply()

    /** حذف خودکار آرشیو بعد از چند روز؛ ۰ یعنی هرگز. */
    var retentionDays: Int
        get() = sp.getInt("retentionDays", 0)
        set(v) = sp.edit().putInt("retentionDays", v).apply()

    /** ۰ = مطابق سیستم، ۱ = روشن، ۲ = تاریک */
    var themeMode: Int
        get() = sp.getInt("themeMode", 0)
        set(v) = sp.edit().putInt("themeMode", v).apply()

    /** The messaging app that was default before Khalvat took the SMS role (used to open messages / compose). */
    var previousSmsApp: String?
        get() = sp.getString("previousSmsApp", null)
        set(v) = sp.edit().putString("previousSmsApp", v).apply()

    fun focusActive(kind: Int, now: Long = System.currentTimeMillis()): Boolean {
        if (focusUntil <= now) return false
        return if (kind == RuleKind.CALL) focusCalls else focusSms
    }
}
