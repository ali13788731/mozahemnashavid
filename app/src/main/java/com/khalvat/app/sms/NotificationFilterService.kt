package com.khalvat.app.sms

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.khalvat.app.data.Db
import com.khalvat.app.data.RuleEngine
import com.khalvat.app.data.RuleKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * اعلان برنامه پیامک را می‌بیند و اگر پیامک مشمول یکی از قوانین باشد، اعلان را پاک می‌کند.
 * (خود پیامک در صندوق برنامه پیامک گوشی باقی می‌ماند.)
 */
class NotificationFilterService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (!isSmsApp(sbn.packageName)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val key = sbn.key

        scope.launch {
            try {
                val db = Db.get(applicationContext)
                val d = RuleEngine.decide(applicationContext, db, RuleKind.SMS, listOf(title))
                if (d != null && d.hideNotification) {
                    cancelNotification(key)
                }
            } catch (_: Throwable) {
                // اعلان عادی بماند
            }
        }
    }

    private fun isSmsApp(pkg: String): Boolean {
        val default = try {
            Telephony.Sms.getDefaultSmsPackage(this)
        } catch (_: Throwable) {
            null
        }
        return pkg == default || pkg in KNOWN_SMS_PACKAGES
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        val KNOWN_SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
            "com.oneplus.mms",
            "com.huawei.message",
            "com.miui.mms"
        )
    }
}
