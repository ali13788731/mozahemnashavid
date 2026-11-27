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
 * هر تماس ورودی با قوانین تماس (روز، ساعت، تاریخ، مخاطب) سنجیده می‌شود.
 * اگر مشمول قانونی باشد، طبق تنظیمات همان قانون:
 *  - بی‌صدا می‌شود (طرف مقابل همچنان بوق آزاد می‌شنود) یا رد می‌شود
 *  - اعلان و صفحه تماس پنهان می‌شود
 *  - از تاریخچه تماس گوشی حذف می‌شود یا می‌ماند
 *  - در آرشیو رمزنگاری‌شده برنامه ذخیره می‌شود یا اصلاً ذخیره نمی‌شود
 *
 * در صورت هر خطا، تماس عادی نمایش داده می‌شود تا تماس مهمی از دست نرود.
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
            // خطا → رفتار عادی
        }

        respondToCall(callDetails, response)
    }
}
