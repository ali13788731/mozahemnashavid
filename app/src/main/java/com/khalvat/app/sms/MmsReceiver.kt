package com.khalvat.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Required for the default-SMS-app role. Picture messages (MMS) are not downloaded by Khalvat;
 * this is stated in the setup screen.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
