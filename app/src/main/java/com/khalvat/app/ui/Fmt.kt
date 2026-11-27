package com.khalvat.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import java.util.Date

/** قالب‌بندی تاریخ شمسی و ارقام فارسی. */
object Fmt {
    private val dateTimeFormat by lazy {
        SimpleDateFormat("yyyy/MM/dd  HH:mm", ULocale("fa_IR@calendar=persian"))
    }
    private val dateFormat by lazy {
        SimpleDateFormat("yyyy/MM/dd", ULocale("fa_IR@calendar=persian"))
    }

    @Synchronized
    fun dateTime(ts: Long): String = dateTimeFormat.format(Date(ts)).faDigits()

    @Synchronized
    fun date(ts: Long): String = dateFormat.format(Date(ts)).faDigits()

    fun minutes(total: Int): String = "%02d:%02d".format(total / 60, total % 60).faDigits()
}

fun String.faDigits(): String = map { c -> if (c in '0'..'9') ('۰' + (c - '0')) else c }.joinToString("")

fun Int.fa(): String = toString().faDigits()

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
