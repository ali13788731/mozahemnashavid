package com.khalvat.app.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.khalvat.app.data.ArchivedSms
import com.khalvat.app.data.Db
import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.RuleEngine
import com.khalvat.app.data.RuleKind
import com.khalvat.app.protect.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Khalvat is the default SMS app, so it is responsible for every incoming message:
 *  - no rule matches        → normal: written to the phone inbox + notification
 *  - rule: keep             → written to the inbox, notification only if not hidden
 *  - rule: private archive  → NOT written to the inbox, copy kept in the encrypted archive
 *  - rule: delete           → not stored anywhere
 * If anything fails the message is stored normally, so nothing is ever lost.
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts.isNullOrEmpty()) return

        val sender = parts[0].originatingAddress.orEmpty()
        val body = parts.joinToString("") { it.messageBody.orEmpty() }
        val sentAt = parts[0].timestampMillis
        val app = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = runCatching { Db.get(app) }.getOrNull()
                val decision = if (db == null) null else runCatching {
                    RuleEngine.decide(app, db, RuleKind.SMS, listOf(sender))
                }.getOrNull()

                var archived = false
                if (decision != null && decision.historyMode == HistoryMode.ARCHIVE && db != null) {
                    archived = runCatching {
                        db.sms().insert(
                            ArchivedSms(
                                sender = sender,
                                name = decision.contactName.orEmpty(),
                                body = body,
                                timestamp = System.currentTimeMillis(),
                                ruleName = decision.ruleName
                            )
                        )
                    }.isSuccess
                }

                val hiddenFromInbox = decision != null && when (decision.historyMode) {
                    HistoryMode.ARCHIVE -> archived
                    HistoryMode.DELETE -> true
                    else -> false
                }
                if (!hiddenFromInbox) saveToInbox(app, sender, body, sentAt)

                val showNotification = decision == null || !decision.hideNotification
                if (showNotification) Notifier.postMessage(app, sender, body)
            } catch (_: Throwable) {
                runCatching { saveToInbox(app, sender, body, sentAt) }
            } finally {
                pending.finish()
            }
        }
    }

    private fun saveToInbox(context: Context, sender: String, body: String, sentAt: Long) {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, sender)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.DATE_SENT, sentAt)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
        }
        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
    }
}
