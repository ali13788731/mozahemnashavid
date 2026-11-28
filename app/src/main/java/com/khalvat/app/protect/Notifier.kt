package com.khalvat.app.protect

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.khalvat.app.MainActivity
import com.khalvat.app.R
import com.khalvat.app.data.Prefs

object Notifier {

    private const val CHANNEL_ALERTS = "alerts"
    private const val CHANNEL_MESSAGES = "messages"
    private const val ID_ALERT = 1001

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.alert_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.msg_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    private fun canPost(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Heads-up alert while anything required is missing; removed once everything is back. */
    fun refreshProtectionAlert(context: Context) {
        val app = context.applicationContext
        val nm = NotificationManagerCompat.from(app)
        if (Requirements.status(app).ok) {
            nm.cancel(ID_ALERT)
            return
        }
        if (!canPost(app)) return
        ensureChannels(app)
        val open = PendingIntent.getActivity(
            app, 0,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(app, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(app.getString(R.string.alert_title))
            .setContentText(app.getString(R.string.alert_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(app.getString(R.string.alert_text)))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        try {
            nm.notify(ID_ALERT, n)
        } catch (_: SecurityException) {
        }
    }

    /** Notification for a message that Khalvat received as the default SMS app and did not hide. */
    fun postMessage(context: Context, sender: String, body: String) {
        val app = context.applicationContext
        if (!canPost(app)) return
        ensureChannels(app)

        val title = lookupName(app, sender) ?: sender.ifEmpty { app.getString(R.string.msg_unknown_sender) }
        val prev = Prefs(app).previousSmsApp
        val launch = prev?.let { app.packageManager.getLaunchIntentForPackage(it) }
            ?: Intent(app, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val open = PendingIntent.getActivity(
            app, sender.hashCode(), launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(app, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(app).notify(sender.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }

    private fun lookupName(context: Context, number: String): String? {
        if (number.isEmpty() || number.any { it.isLetter() }) return null
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)
            )
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Throwable) {
            null
        }
    }
}
