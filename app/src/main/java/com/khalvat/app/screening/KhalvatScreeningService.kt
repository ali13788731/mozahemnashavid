package com.khalvat.app.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.khalvat.app.data.ArchivedCall
import com.khalvat.app.data.Db
import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.RuleEngine
import com.khalvat.app.data.RuleKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Every incoming call is checked against the call rules (day, time, date, contact).
 * A matching rule can silence or reject the call, hide its notification/screen,
 * remove it from the phone call log and/or keep a copy in the encrypted archive.
 * On any error the call is shown normally so an important call is never lost.
 */
class KhalvatScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        var response = CallResponse.Builder().build()

        try {
            if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
                val raw = callDetails.handle?.schemeSpecificPart
                val context = applicationContext

                val decision = runBlocking(Dispatchers.IO) {
                    val db = Db.get(context)
                    val d = RuleEngine.decide(context, db, RuleKind.CALL, listOfNotNull(raw))
                    if (d != null && d.historyMode == HistoryMode.ARCHIVE) {
                        db.calls().insert(
                            ArchivedCall(
                                number = raw.orEmpty(),
                                name = d.contactName.orEmpty(),
                                timestamp = System.currentTimeMillis(),
                                ruleName = d.ruleName
                            )
                        )
                    }
                    d
                }

                if (decision != null) {
                    val builder = CallResponse.Builder()
                    if (decision.reject) {
                        builder.setDisallowCall(true).setRejectCall(true)
                    } else if (decision.silence) {
                        builder.setSilenceCall(true)
                    }
                    builder.setSkipNotification(decision.hideNotification)
                    builder.setSkipCallLog(decision.historyMode != HistoryMode.KEEP)
                    response = builder.build()
                }
            }
        } catch (_: Throwable) {
            // error → normal behaviour
        }

        respondToCall(callDetails, response)
    }
}
