package com.khalvat.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.khalvat.app.data.Db
import com.khalvat.app.data.Prefs
import com.khalvat.app.data.RuleKind
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(onNavigate: (Dest) -> Unit) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val prefs = remember { Prefs(ctx) }

    val callRules by db.rules().observeActiveCount(RuleKind.CALL).collectAsState(initial = 0)
    val smsRules by db.rules().observeActiveCount(RuleKind.SMS).collectAsState(initial = 0)
    val archivedCalls by db.calls().observeCount().collectAsState(initial = 0)
    val archivedSms by db.sms().observeCount().collectAsState(initial = 0)

    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }

    val roleManager = remember { ctx.getSystemService(RoleManager::class.java) }
    val roleHeld = remember(tick) { roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) }
    val contactsOk = remember(tick) { granted(ctx, Manifest.permission.READ_CONTACTS) }
    val smsOk = remember(tick) { granted(ctx, Manifest.permission.RECEIVE_SMS) }
    val listenerOk = remember(tick) {
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    val allDone = roleHeld && contactsOk && smsOk && listenerOk

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    var focusUntil by remember { mutableLongStateOf(prefs.focusUntil) }
    val focusActive = focusUntil > System.currentTimeMillis()
    var callsOn by remember { mutableStateOf(prefs.callsEnabled) }
    var smsOn by remember { mutableStateOf(prefs.smsEnabled) }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // وضعیت کلی
        item {
            GradientCard(Modifier.fillMaxWidth()) {
                Text("وضعیت محافظت", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (allDone) "همه‌چیز فعال و آماده است" else "نیاز به راه‌اندازی دارد",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    Stat(callRules.fa(), "قانون تماس فعال", Modifier.weight(1f)) { onNavigate(Dest.Calls) }
                    Stat(smsRules.fa(), "قانون پیامک فعال", Modifier.weight(1f)) { onNavigate(Dest.Sms) }
                    Stat(archivedCalls.fa(), "تماس آرشیو", Modifier.weight(1f)) { onNavigate(Dest.Archive) }
                    Stat(archivedSms.fa(), "پیامک آرشیو", Modifier.weight(1f)) { onNavigate(Dest.Archive) }
                }
            }
        }

        // حالت تمرکز
        item {
            GradientCard(Modifier.fillMaxWidth().animateContentSize(), brush = Brand.gradientWarm) {
                Text("حالت تمرکز", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (focusActive) "فعال تا ${Fmt.dateTime(focusUntil)}"
                    else "همه تماس‌ها و پیامک‌ها بی‌صدا و پنهان می‌شوند (به‌جز مخاطبین ویژه).",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                if (focusActive) {
                    FilledTonalButton(onClick = {
                        prefs.focusUntil = 0L
                        focusUntil = 0L
                    }) { Text("غیرفعال کردن") }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FocusButton("۱ ساعت") { focusUntil = startFocus(prefs, hours = 1) }
                        FocusButton("۳ ساعت") { focusUntil = startFocus(prefs, hours = 3) }
                        FocusButton("۸ ساعت") { focusUntil = startFocus(prefs, hours = 8) }
                        FocusButton("تا ۷ صبح") { focusUntil = startFocus(prefs, untilMorning = true) }
                    }
                }
            }
        }

        // کلیدهای اصلی
        item {
            AppCard(Modifier.fillMaxWidth()) {
                SwitchRow(
                    title = "محافظت تماس‌ها",
                    subtitle = "با خاموش شدن، قوانین تماس اجرا نمی‌شوند",
                    checked = callsOn,
                    onChecked = { callsOn = it; prefs.callsEnabled = it }
                )
                SwitchRow(
                    title = "محافظت پیامک‌ها",
                    subtitle = "با خاموش شدن، قوانین پیامک اجرا نمی‌شوند",
                    checked = smsOn,
                    onChecked = { smsOn = it; prefs.smsEnabled = it }
                )
            }
        }

        // راه‌اندازی
        item {
            AppCard(Modifier.fillMaxWidth().animateContentSize()) {
                Text("راه‌اندازی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = allDone) {
                    Text(
                        "همه دسترسی‌ها داده شده است.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SetupRow(
                    title = "برنامه غربالگری تماس",
                    description = "برای بی‌صدا کردن و پنهان کردن تماس‌ها لازم است.",
                    done = roleHeld
                ) {
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                    }
                }
                SetupRow(
                    title = "دسترسی به مخاطبین",
                    description = "برای تشخیص تماس مخاطبین ذخیره‌شده و انتخاب مخاطب.",
                    done = contactsOk
                ) { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                SetupRow(
                    title = "دسترسی دریافت پیامک",
                    description = "برای ذخیره کپی پیامک‌ها در آرشیو (اختیاری).",
                    done = smsOk
                ) { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) }
                SetupRow(
                    title = "دسترسی به اعلان‌ها",
                    description = "برای پنهان کردن اعلان پیامک‌ها لازم است.",
                    done = listenerOk
                ) { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            }
        }
    }
}

private fun granted(ctx: android.content.Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

private fun startFocus(prefs: Prefs, hours: Int = 0, untilMorning: Boolean = false): Long {
    val until = if (untilMorning) {
        Calendar.getInstance().apply {
            if (get(Calendar.HOUR_OF_DAY) >= 7) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } else {
        System.currentTimeMillis() + hours * 3_600_000L
    }
    prefs.focusUntil = until
    return until
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            label,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2
        )
    }
}

@Composable
private fun FocusButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) { Text(label) }
}

@Composable
private fun SetupRow(title: String, description: String, done: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (done) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!done) {
            TextButton(onClick = onClick) { Text("فعال‌سازی") }
        }
    }
}
