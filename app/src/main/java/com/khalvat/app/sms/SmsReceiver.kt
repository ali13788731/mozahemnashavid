package com.khalvat.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.khalvat.app.data.ArchivedSms
import com.khalvat.app.data.Db
import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.RuleEngine
import com.khalvat.app.data.RuleKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * متن واقعی پیامک را می‌گیرد و اگر مشمول قانونی باشد و حالت «آرشیو» انتخاب شده باشد،
 * یک کپی در آرشیو رمزنگاری‌شده ذخیره می‌کند.
 * پنهان شدن اعلان را NotificationFilterService انجام می‌دهد.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val appContext = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Db.get(appContext)
                val d = RuleEngine.decide(appContext, db, RuleKind.SMS, listOf(sender))
                if (d != null && d.historyMode == HistoryMode.ARCHIVE) {
                    db.sms().insert(
                        ArchivedSms(
                            sender = sender,
                            name = d.contactName.orEmpty(),
                            body = body,
                            timestamp = System.currentTimeMillis(),
                            ruleName = d.ruleName
                        )
                    )
                }
            } catch (_: Throwable) {
                // نادیده گرفتن؛ پیامک عادی دریافت می‌شود
            } finally {
                pending.finish()
            }
        }
    }
}
