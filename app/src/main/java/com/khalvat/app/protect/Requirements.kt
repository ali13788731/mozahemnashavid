package com.khalvat.app.protect

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Everything Khalvat needs before it can hide anything:
 *  1. runtime permissions (contacts, SMS, notifications)
 *  2. the "call screening app" role
 *  3. the "default SMS app" role
 * If any of them is missing the whole app is blocked and an alert notification is shown.
 */
object Requirements {

    val runtimePermissions: List<String> = buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.RECEIVE_MMS)
        add(Manifest.permission.RECEIVE_WAP_PUSH)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    data class Status(
        val permissions: Boolean,
        val callRole: Boolean,
        val smsRole: Boolean
    ) {
        val ok: Boolean get() = permissions && callRole && smsRole
    }

    fun missingPermissions(context: Context): List<String> =
        runtimePermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun status(context: Context): Status {
        val roles = context.getSystemService(RoleManager::class.java)
        return Status(
            permissions = missingPermissions(context).isEmpty(),
            callRole = roles?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true,
            smsRole = roles?.isRoleHeld(RoleManager.ROLE_SMS) == true
        )
    }
}
