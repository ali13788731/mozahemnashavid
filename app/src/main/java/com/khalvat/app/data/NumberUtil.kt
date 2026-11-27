package com.khalvat.app.data

object NumberUtil {
    /**
     * کلید تطبیق:
     *  - اگر ورودی حرف دارد (نام فرستنده پیامک مثل Digikala) → همان متن با حروف کوچک
     *  - در غیر این صورت فقط رقم‌ها (فارسی و انگلیسی) و ۱۰ رقم آخر،
     *    تا +98912…، 0912… و 912… یکسان تطبیق داده شوند.
     */
    fun key(raw: String): String {
        val text = raw.trim()
        if (text.any { it.isLetter() }) return text.lowercase()
        val digits = text.filter { it.isDigit() }.map { it.digitToInt() }.joinToString("")
        return if (digits.length > 10) digits.takeLast(10) else digits
    }
}
