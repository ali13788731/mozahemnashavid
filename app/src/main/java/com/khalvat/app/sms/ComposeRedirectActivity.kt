package com.khalvat.app.sms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.khalvat.app.MainActivity
import com.khalvat.app.data.Prefs

/** Hands "new message" links (sms:, smsto:) to the messaging app the user had before Khalvat. */
class ComposeRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prev = Prefs(this).previousSmsApp
        val forward = Intent(intent).apply {
            component = null
            setPackage(prev)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val started = prev != null && runCatching { startActivity(forward) }.isSuccess
        if (!started) {
            startActivity(
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        finish()
    }
}
