package com.khalvat.app.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Required for the default-SMS-app role ("reply with message" from the call screen). */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
