package com.khalvat.app.ui

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.khalvat.app.R
import com.khalvat.app.data.Prefs
import com.khalvat.app.protect.Requirements

/**
 * Shown on first launch and whenever something required is missing.
 * "Allow everything" walks through: permissions → call screening role → default SMS app.
 */
@Composable
fun SetupScreen(onReady: () -> Unit) {
    val ctx = LocalContext.current
    val roles = remember { ctx.getSystemService(RoleManager::class.java) }

    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val status = remember(tick) { Requirements.status(ctx) }
    val missingPerms = remember(tick) { Requirements.missingPermissions(ctx) }

    var chain by remember { mutableStateOf(false) }
    var waiting by remember { mutableStateOf(false) }
    var permDenied by remember { mutableIntStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        waiting = false
        if (Requirements.missingPermissions(ctx).isNotEmpty()) {
            chain = false
            permDenied++
        }
        tick++
    }
    var requested by remember { mutableStateOf("") }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        waiting = false
        if (roles?.isRoleHeld(requested) != true) chain = false
        tick++
    }

    fun requestRole(role: String) {
        waiting = true
        requested = role
        if (role == RoleManager.ROLE_SMS) {
            val current = Telephony.Sms.getDefaultSmsPackage(ctx)
            if (current != null && current != ctx.packageName) Prefs(ctx).previousSmsApp = current
        }
        if (roles != null && roles.isRoleAvailable(role)) {
            roleLauncher.launch(roles.createRequestRoleIntent(role))
        } else {
            waiting = false
            chain = false
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
    }

    fun launchNext() {
        val s = Requirements.status(ctx)
        when {
            !s.permissions -> {
                waiting = true
                permLauncher.launch(Requirements.missingPermissions(ctx).toTypedArray())
            }
            !s.callRole -> requestRole(RoleManager.ROLE_CALL_SCREENING)
            !s.smsRole -> requestRole(RoleManager.ROLE_SMS)
            else -> chain = false
        }
    }

    LaunchedEffect(tick, chain) {
        if (status.ok) {
            onReady()
        } else if (chain && !waiting) {
            launchNext()
        }
    }

    val activity = ctx.findActivity()
    val needAppSettings = missingPerms.isNotEmpty() && permDenied > 0 && activity != null &&
        missingPerms.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            KhalvatLogo(Modifier.size(96.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepCard(Icons.Default.Lock, R.string.setup_perm_title, R.string.setup_perm_desc, status.permissions)
                StepCard(Icons.Default.Call, R.string.setup_call_title, R.string.setup_call_desc, status.callRole)
                StepCard(Icons.Default.Email, R.string.setup_sms_title, R.string.setup_sms_desc, status.smsRole)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(
                    if (needAppSettings) R.string.setup_denied_hint else R.string.setup_all_required
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (needAppSettings) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (needAppSettings) {
                OutlinedButton(
                    onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", ctx.packageName, null))
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(stringResource(R.string.setup_open_settings)) }
                Spacer(Modifier.height(8.dp))
            }
            val started = status.permissions || status.callRole || status.smsRole
            Button(
                onClick = { chain = true; if (!waiting) launchNext() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    stringResource(if (started) R.string.setup_continue else R.string.setup_grant_all),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StepCard(icon: ImageVector, title: Int, desc: Int, done: Boolean) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (done) Icons.Default.Check else icon,
                if (done) Brand.call else Brand.hero
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
